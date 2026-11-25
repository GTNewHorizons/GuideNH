package com.hfstudio.guidenh.client.capture.geometry;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureResizeEdges;

public class CaptureSelectionBounds {

    private CaptureSelectionBounds() {}

    public static CaptureRect create(CaptureRect origin, double dx, double dy, CaptureRect bounds, boolean keepRatio) {
        if (keepRatio) {
            double edge = Math.max(Math.abs(dx), Math.abs(dy));
            edge = Math.min(edge, available(origin.x(), dx, bounds.x(), bounds.right()));
            edge = Math.min(edge, available(origin.y(), dy, bounds.y(), bounds.bottom()));
            dx = Math.copySign(edge, dx);
            dy = Math.copySign(edge, dy);
        }
        return CaptureRect.between(
            origin.x(),
            origin.y(),
            Math.clamp(origin.x() + dx, bounds.x(), bounds.right()),
            Math.clamp(origin.y() + dy, bounds.y(), bounds.bottom()));
    }

    public static CaptureRect resize(CaptureRect initial, CaptureResizeEdges edges, double dx, double dy,
        CaptureRect bounds, boolean keepRatio) {
        double left = initial.x() + (edges.left() ? dx : 0);
        double right = initial.right() + (edges.right() ? dx : 0);
        double top = initial.y() + (edges.top() ? dy : 0);
        double bottom = initial.bottom() + (edges.bottom() ? dy : 0);
        if (!keepRatio || initial.width() == 0 || initial.height() == 0) {
            return CaptureRect.between(
                Math.clamp(left, bounds.x(), bounds.right()),
                Math.clamp(top, bounds.y(), bounds.bottom()),
                Math.clamp(right, bounds.x(), bounds.right()),
                Math.clamp(bottom, bounds.y(), bounds.bottom()));
        }
        double anchorX = edges.left() ? initial.right() : initial.x();
        double anchorY = edges.top() ? initial.bottom() : initial.y();
        double directionX = edges.left() ? left - anchorX : right - anchorX;
        double directionY = edges.top() ? top - anchorY : bottom - anchorY;
        double scale = edges.left() || edges.right() ? Math.abs(right - left) / initial.width()
            : Math.abs(bottom - top) / initial.height();
        scale = Math.min(scale, available(anchorX, directionX, bounds.x(), bounds.right()) / initial.width());
        scale = Math.min(scale, available(anchorY, directionY, bounds.y(), bounds.bottom()) / initial.height());
        double width = Math.copySign(initial.width() * scale, directionX);
        double height = Math.copySign(initial.height() * scale, directionY);
        return CaptureRect.between(anchorX, anchorY, anchorX + width, anchorY + height);
    }

    private static double available(double anchor, double direction, double start, double end) {
        return direction < 0 ? anchor - start : end - anchor;
    }
}
