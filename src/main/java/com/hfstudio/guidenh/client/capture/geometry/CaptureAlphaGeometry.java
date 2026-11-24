package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureAlphaGeometry {

    private static final int CELL_SIZE = 8;
    private static final int ALPHA_THRESHOLD = 8;

    private CaptureAlphaGeometry() {}

    // Pixel coverage supplies geometry for renderers which expose no widget or draw-command metadata.
    public static List<CaptureTarget> collect(CaptureFrame frame, CaptureViewport viewport, String layer) {
        return collect(frame, viewport, layer, null);
    }

    public static List<CaptureTarget> collect(CaptureFrame frame, CaptureViewport viewport, String layer,
        CaptureRect excluded) {
        CaptureRect excludedPixels = excluded == null ? null : viewport.pixels(excluded);
        int cellWidth = Math.max(1, (int) Math.ceil(CELL_SIZE * viewport.scaleX()));
        int cellHeight = Math.max(1, (int) Math.ceil(CELL_SIZE * viewport.scaleY()));
        int columns = (frame.width() + cellWidth - 1) / cellWidth;
        int rows = (frame.height() + cellHeight - 1) / cellHeight;
        int size = Math.multiplyExact(columns, rows);
        int[] parent = new int[size];
        int[] left = new int[size];
        int[] top = new int[size];
        int[] right = new int[size];
        int[] bottom = new int[size];
        for (int y = 0; y < frame.height(); y++) {
            int offset = y / cellHeight * columns;
            for (int x = 0; x < frame.width(); x++) {
                if (excludedPixels != null && excludedPixels.contains(x, y)) continue;
                if (frame.argb(x, y) >>> 24 < ALPHA_THRESHOLD) continue;
                int cell = offset + x / cellWidth;
                if (right[cell] == 0) {
                    parent[cell] = cell;
                    left[cell] = x;
                    top[cell] = y;
                }
                left[cell] = Math.min(left[cell], x);
                right[cell] = Math.max(right[cell], x + 1);
                bottom[cell] = y + 1;
            }
        }
        for (int cell = 0; cell < size; cell++) {
            if (right[cell] == 0) continue;
            int x = cell % columns;
            if (x > 0) join(cell, cell - 1, parent, left, top, right, bottom);
            if (cell >= columns) {
                join(cell, cell - columns, parent, left, top, right, bottom);
                if (x > 0) join(cell, cell - columns - 1, parent, left, top, right, bottom);
                if (x + 1 < columns) join(cell, cell - columns + 1, parent, left, top, right, bottom);
            }
        }
        List<CaptureTarget> targets = new ArrayList<>();
        for (int cell = 0; cell < size; cell++) {
            if (right[cell] == 0 || root(cell, parent) != cell) continue;
            CaptureRect bounds = new CaptureRect(
                left[cell] / viewport.scaleX(),
                top[cell] / viewport.scaleY(),
                (right[cell] - left[cell]) / viewport.scaleX(),
                (bottom[cell] - top[cell]) / viewport.scaleY());
            if (bounds.width() < 2 || bounds.height() < 2) continue;
            boolean backdrop = bounds.width() * bounds.height()
                >= viewport.logicalWidth() * (double) viewport.logicalHeight() * 0.9;
            targets.add(
                new CaptureTarget(
                    "pixels:" + cell,
                    "minecraft:screen",
                    backdrop ? CaptureTarget.WINDOW : CaptureTarget.COMPONENT,
                    CaptureAlphaGeometry.class.getName(),
                    "Visible component",
                    bounds,
                    bounds,
                    0,
                    backdrop ? -95 : 0).withContent(layer, false));
        }
        return List.copyOf(targets);
    }

    private static int root(int cell, int[] parent) {
        while (parent[cell] != cell) {
            parent[cell] = parent[parent[cell]];
            cell = parent[cell];
        }
        return cell;
    }

    private static void join(int first, int second, int[] parent, int[] left, int[] top, int[] right, int[] bottom) {
        if (right[second] == 0) return;
        int a = root(first, parent);
        int b = root(second, parent);
        if (a == b) return;
        int keep = Math.min(a, b);
        int merge = Math.max(a, b);
        parent[merge] = keep;
        left[keep] = Math.min(left[keep], left[merge]);
        top[keep] = Math.min(top[keep], top[merge]);
        right[keep] = Math.max(right[keep], right[merge]);
        bottom[keep] = Math.max(bottom[keep], bottom[merge]);
    }
}
