package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class CaptureGeometryIndex {

    private static final int CELL_SIZE = 64;
    private static final Comparator<CaptureTarget> PICK_ORDER = Comparator.comparingInt(CaptureTarget::zOrder)
        .reversed()
        .thenComparing(
            Comparator.comparingInt(CaptureTarget::priority)
                .reversed())
        .thenComparingDouble(
            target -> target.visibleBounds()
                .width()
                * target.visibleBounds()
                    .height());

    private final CaptureGeometry geometry;
    private final CaptureGeometry sourceGeometry;
    private final boolean includeSlotBorders;
    private final Map<Long, List<CaptureTarget>> cells = new HashMap<>();
    private final Map<String, CaptureTarget> tracked = new HashMap<>();

    public CaptureGeometryIndex(CaptureGeometry geometry) {
        this(geometry, true);
    }

    public CaptureGeometryIndex(CaptureGeometry geometry, boolean includeSlotBorders) {
        sourceGeometry = geometry;
        this.includeSlotBorders = includeSlotBorders;
        if (includeSlotBorders) this.geometry = geometry;
        else {
            List<CaptureTarget> targets = geometry.targets()
                .stream()
                .map(CaptureGeometryIndex::slotContents)
                .filter(
                    target -> !target.visibleBounds()
                        .isEmpty())
                .toList();
            this.geometry = new CaptureGeometry(targets.hashCode(), geometry.viewport(), targets);
        }
        for (CaptureTarget target : this.geometry.targets()) {
            if (!target.trackingId()
                .isEmpty()) tracked.put(target.trackingId(), target);
            CaptureRect bounds = target.visibleBounds()
                .intersect(
                    geometry.viewport()
                        .bounds());
            if (bounds.isEmpty()) continue;
            int lastX = cell(Math.nextDown(bounds.right()));
            int lastY = cell(Math.nextDown(bounds.bottom()));
            for (int x = cell(bounds.x()); x <= lastX; x++) {
                for (int y = cell(bounds.y()); y <= lastY; y++) {
                    cells.computeIfAbsent(key(x, y), ignored -> new ArrayList<>())
                        .add(target);
                }
            }
        }
    }

    public CaptureGeometry geometry() {
        return geometry;
    }

    public CaptureGeometry sourceGeometry() {
        return sourceGeometry;
    }

    public boolean includeSlotBorders() {
        return includeSlotBorders;
    }

    private static CaptureTarget slotContents(CaptureTarget target) {
        if (!target.kind()
            .equals(CaptureTarget.SLOT)) return target;
        CaptureRect original = target.bounds();
        double insetX = original.width() / 18;
        double insetY = original.height() / 18;
        CaptureRect contents = new CaptureRect(
            original.x() + insetX,
            original.y() + insetY,
            original.width() - 2 * insetX,
            original.height() - 2 * insetY);
        return new CaptureTarget(
            target.id(),
            target.parentId(),
            target.kind(),
            target.className(),
            target.label(),
            contents,
            contents.intersect(target.visibleBounds()),
            target.zOrder(),
            target.priority(),
            target.ownerIdentity(),
            target.contentLayer(),
            target.playerInventory(),
            target.providerId());
    }

    public CaptureTarget tracked(String id) {
        return tracked.get(id);
    }

    public List<CaptureTarget> candidates(double x, double y, CaptureSnapOptions options) {
        List<CaptureTarget> candidates = cells.get(key(cell(x), cell(y)));
        if (candidates == null || !options.enabled()) return List.of();
        return candidates.stream()
            .filter(options::accepts)
            .filter(
                target -> target.visibleBounds()
                    .contains(x, y))
            .sorted(PICK_ORDER)
            .toList();
    }

    private static int cell(double value) {
        return (int) Math.floor(value / CELL_SIZE);
    }

    private static long key(int x, int y) {
        return (long) x << 32 | y & 0xffffffffL;
    }
}
