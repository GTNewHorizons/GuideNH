package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureDrawCollector {

    private static final int MAX_TARGETS = 16384;
    private final CaptureDrawProjection projection = new CaptureDrawProjection();
    private final Map<CaptureRect, CaptureTarget> targets = new LinkedHashMap<>();

    public void clear() {
        targets.clear();
    }

    public void recordText(String text, int x, int y, int width, int height, boolean shadow, CaptureViewport viewport,
        String layer) {
        if (text == null || text.isEmpty() || width <= 0) return;
        CaptureRect bounds = projection
            .project(x, y, 0, x + width + (shadow ? 1 : 0), y + height + (shadow ? 1 : 0), 0, viewport);
        record(bounds, "Text", layer, viewport);
    }

    public void recordVertices(int[] vertices, int count, int used, CaptureViewport viewport, String layer) {
        if (count <= 0 || used <= 0 || vertices == null || used > vertices.length || used % count != 0) return;
        int stride = used / count;
        if (stride < 3) return;
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double near = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        double far = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < used; index += stride) {
            float x = Float.intBitsToFloat(vertices[index]);
            float y = Float.intBitsToFloat(vertices[index + 1]);
            float z = Float.intBitsToFloat(vertices[index + 2]);
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return;
            left = Math.min(left, x);
            right = Math.max(right, x);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
            near = Math.min(near, z);
            far = Math.max(far, z);
        }
        record(projection.project(left, top, near, right, bottom, far, viewport), "Drawn component", layer, viewport);
    }

    private void record(CaptureRect bounds, String label, String layer, CaptureViewport viewport) {
        if (bounds == null || bounds.isEmpty() || targets.size() >= MAX_TARGETS) return;
        boolean backdrop = bounds.width() * bounds.height()
            >= viewport.logicalWidth() * (double) viewport.logicalHeight() * 0.9;
        targets.putIfAbsent(
            bounds,
            new CaptureTarget(
                "draw:" + targets.size(),
                "minecraft:screen",
                backdrop ? CaptureTarget.WINDOW : CaptureTarget.COMPONENT,
                CaptureDrawCollector.class.getName(),
                label,
                bounds,
                bounds,
                0,
                backdrop ? -90 : 5).withContent(layer, false));
    }

    public CaptureGeometry merge(CaptureGeometry geometry, CaptureFrame frame) {
        List<CaptureTarget> combined = new ArrayList<>(geometry.targets());
        for (CaptureTarget target : targets.values()) {
            if (frame == null) {
                combined.add(target);
                continue;
            }
            CaptureRect area = geometry.viewport()
                .pixels(target.visibleBounds());
            int left = Math.max(0, (int) area.x());
            int top = Math.max(0, (int) area.y());
            int right = Math.min(frame.width(), (int) area.right());
            int bottom = Math.min(frame.height(), (int) area.bottom());
            boolean visible = false;
            // Probe likely occupied pixels before scanning sparse transparent rectangles.
            for (int row = 0; row < 3 && !visible && bottom > top; row++) {
                int y = top + (bottom - top - 1) * row / 2;
                for (int column = 0; column < 3 && right > left; column++) {
                    int x = left + (right - left - 1) * column / 2;
                    if (frame.argb(x, y) >>> 24 != 0) {
                        visible = true;
                        break;
                    }
                }
            }
            for (int y = top; y < bottom && !visible; y++) {
                for (int x = left; x < right; x++) {
                    if (frame.argb(x, y) >>> 24 != 0) {
                        visible = true;
                        break;
                    }
                }
            }
            if (visible) combined.add(target);
        }
        return new CaptureGeometry(combined.hashCode(), geometry.viewport(), combined);
    }
}
