package com.hfstudio.guidenh.client.capture.session;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureResizeEdges;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.geometry.CaptureSnapResolver;

public class CaptureSelectionController {

    private final List<CaptureRegion> regions = new ArrayList<>();
    private final CaptureSelectionHistory history = new CaptureSelectionHistory();
    private final CaptureSnapResolver snapResolver = new CaptureSnapResolver();
    private CaptureRect bounds;
    private long nextId;
    private long activeId = -1;
    private CaptureSelectionDrag drag;

    public CaptureSelectionController(CaptureRect bounds) {
        this.bounds = bounds;
    }

    public List<CaptureRegion> regions() {
        return List.copyOf(regions);
    }

    public CaptureRegion active() {
        for (CaptureRegion region : regions) if (region.id() == activeId) return region;
        return null;
    }

    public CaptureSelectionHistory history() {
        return history;
    }

    public boolean dragging() {
        return drag != null;
    }

    public void setBounds(CaptureRect rectangle) {
        bounds = rectangle;
        for (int index = 0; index < regions.size(); index++) {
            CaptureRegion region = regions.get(index);
            regions.set(
                index,
                region.withBounds(
                    region.bounds()
                        .clamp(bounds)));
        }
        drag = null;
    }

    public CaptureResizeEdges handleAt(double x, double y) {
        CaptureRegion active = active();
        if (active == null || active.bounds()
            .isEmpty()) return CaptureResizeEdges.NONE;
        CaptureRect rectangle = active.bounds();
        double leftDistance = Math.abs(x - rectangle.x());
        double rightDistance = Math.abs(x - rectangle.right());
        double topDistance = Math.abs(y - rectangle.y());
        double bottomDistance = Math.abs(y - rectangle.bottom());
        boolean nearX = x >= rectangle.x() - 4 && x <= rectangle.right() + 4;
        boolean nearY = y >= rectangle.y() - 4 && y <= rectangle.bottom() + 4;
        return new CaptureResizeEdges(
            nearY && leftDistance <= 4 && leftDistance <= rightDistance,
            nearY && rightDistance <= 4 && rightDistance < leftDistance,
            nearX && topDistance <= 4 && topDistance <= bottomDistance,
            nearX && bottomDistance <= 4 && bottomDistance < topDistance);
    }

    public void begin(double x, double y, boolean additive, CaptureTarget target) {
        history.record(snapshot());
        snapResolver.reset();
        CaptureRegion selected = active();
        CaptureResizeEdges edges = additive ? CaptureResizeEdges.NONE : handleAt(x, y);
        if (!edges.isEmpty() && selected != null) {
            drag = new CaptureSelectionDrag(
                selected.id(),
                CaptureSelectionDrag.RESIZE,
                x,
                y,
                selected.bounds(),
                edges,
                null);
            return;
        }
        if (!additive) {
            for (int index = regions.size() - 1; index >= 0; index--) {
                CaptureRegion region = regions.get(index);
                if (!region.bounds()
                    .contains(x, y)) continue;
                activeId = region.id();
                drag = new CaptureSelectionDrag(
                    region.id(),
                    CaptureSelectionDrag.MOVE,
                    x,
                    y,
                    region.bounds(),
                    CaptureResizeEdges.NONE,
                    null);
                return;
            }
            regions.clear();
        }
        activeId = ++nextId;
        CaptureRect initial = new CaptureRect(
            Math.clamp(x, bounds.x(), bounds.right()),
            Math.clamp(y, bounds.y(), bounds.bottom()),
            0,
            0);
        regions.add(new CaptureRegion(activeId, initial, target == null ? "" : target.id(), false));
        drag = new CaptureSelectionDrag(
            activeId,
            CaptureSelectionDrag.CREATE,
            x,
            y,
            initial,
            CaptureResizeEdges.NONE,
            target);
    }

    public void move(double x, double y, boolean keepRatio, List<CaptureTarget> targets, CaptureSnapOptions options) {
        if (drag == null) return;
        double dx = x - drag.originX();
        double dy = y - drag.originY();
        CaptureRect initial = drag.initialBounds();
        CaptureRect changed;
        CaptureResizeEdges edges = drag.edges();
        boolean moving = drag.kind()
            .equals(CaptureSelectionDrag.MOVE);
        if (moving) {
            changed = initial.move(dx, dy);
        } else if (drag.kind()
            .equals(CaptureSelectionDrag.CREATE)) {
                if (keepRatio) {
                    double edge = Math.max(Math.abs(dx), Math.abs(dy));
                    dx = Math.copySign(edge, dx);
                    dy = Math.copySign(edge, dy);
                }
                changed = CaptureRect.between(initial.x(), initial.y(), initial.x() + dx, initial.y() + dy);
                edges = new CaptureResizeEdges(dx < 0, dx >= 0, dy < 0, dy >= 0);
            } else {
                double left = initial.x() + (edges.left() ? dx : 0);
                double right = initial.right() + (edges.right() ? dx : 0);
                double top = initial.y() + (edges.top() ? dy : 0);
                double bottom = initial.bottom() + (edges.bottom() ? dy : 0);
                if (keepRatio && initial.height() > 0) {
                    double ratio = initial.width() / initial.height();
                    if (edges.left() || edges.right()) {
                        double height = Math.abs(right - left) / ratio;
                        if (edges.top()) top = bottom - height;
                        else bottom = top + height;
                    } else {
                        right = left + Math.abs(bottom - top) * ratio;
                    }
                }
                changed = CaptureRect.between(left, top, right, bottom);
            }
        if (!keepRatio) changed = snapResolver.snap(changed, targets, options, edges, moving);
        replace(drag.regionId(), changed.clamp(bounds));
    }

    public void finish(double x, double y) {
        if (drag == null) return;
        if (drag.kind()
            .equals(CaptureSelectionDrag.CREATE) && drag.clickTarget() != null
            && Math.hypot(x - drag.originX(), y - drag.originY()) <= 3) {
            replace(
                drag.regionId(),
                drag.clickTarget()
                    .visibleBounds()
                    .intersect(bounds));
        }
        regions.removeIf(
            region -> region.bounds()
                .isEmpty());
        if (active() == null) activeId = regions.isEmpty() ? -1
            : regions.getLast()
                .id();
        drag = null;
        snapResolver.reset();
    }

    public void cancelDrag() {
        if (drag == null) return;
        drag = null;
        undo();
    }

    public void redraw() {
        if (regions.isEmpty()) return;
        history.record(snapshot());
        if (regions.size() > 1) removeActive();
        else {
            regions.clear();
            activeId = -1;
        }
        drag = null;
    }

    public void deleteActive() {
        if (active() == null) return;
        history.record(snapshot());
        removeActive();
    }

    private void removeActive() {
        regions.removeIf(region -> region.id() == activeId);
        activeId = regions.isEmpty() ? -1
            : regions.getLast()
                .id();
    }

    public void select(CaptureRect rectangle, boolean additive) {
        history.record(snapshot());
        if (!additive) regions.clear();
        activeId = ++nextId;
        regions.add(new CaptureRegion(activeId, rectangle.intersect(bounds), "", false));
    }

    public void adjustActive(CaptureRect rectangle) {
        CaptureRegion selected = active();
        if (selected == null) return;
        history.record(snapshot());
        replace(selected.id(), rectangle.clamp(bounds));
    }

    public void nudge(double dx, double dy, boolean all) {
        if (regions.isEmpty()) return;
        history.record(snapshot());
        if (!all) {
            CaptureRegion selected = active();
            if (selected != null) replace(
                selected.id(),
                selected.bounds()
                    .move(dx, dy)
                    .clamp(bounds));
            return;
        }
        CaptureRect union = regions.getFirst()
            .bounds();
        for (CaptureRegion region : regions) union = union.union(region.bounds());
        CaptureRect moved = union.move(dx, dy)
            .clamp(bounds);
        double actualX = moved.x() - union.x();
        double actualY = moved.y() - union.y();
        for (int index = 0; index < regions.size(); index++) {
            CaptureRegion region = regions.get(index);
            regions.set(
                index,
                region.withBounds(
                    region.bounds()
                        .move(actualX, actualY)));
        }
    }

    public void duplicateActive() {
        CaptureRegion selected = active();
        if (selected == null) return;
        select(
            selected.bounds()
                .move(8, 8)
                .clamp(bounds),
            true);
    }

    public void undo() {
        restore(history.undo(snapshot()));
    }

    public void redo() {
        restore(history.redo(snapshot()));
    }

    public CaptureSelectionSnapshot snapshot() {
        return new CaptureSelectionSnapshot(regions, activeId);
    }

    public void restore(CaptureSelectionSnapshot snapshot) {
        if (snapshot == null) return;
        regions.clear();
        for (CaptureRegion region : snapshot.regions()) regions.add(
            region.withBounds(
                region.bounds()
                    .clamp(bounds)));
        activeId = snapshot.activeId();
        drag = null;
        snapResolver.reset();
    }

    private void replace(long id, CaptureRect rectangle) {
        for (int index = 0; index < regions.size(); index++) {
            CaptureRegion region = regions.get(index);
            if (region.id() == id) {
                regions.set(index, region.withBounds(rectangle));
                return;
            }
        }
    }
}
