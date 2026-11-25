package com.hfstudio.guidenh.client.capture.api;

import java.util.ArrayList;
import java.util.List;

public record CaptureRect(double x, double y, double width, double height) {

    public CaptureRect {
        if (!Double.isFinite(x) || !Double.isFinite(y)
            || !Double.isFinite(width)
            || !Double.isFinite(height)
            || !Double.isFinite(x + width)
            || !Double.isFinite(y + height)
            || width < 0
            || height < 0) {
            throw new IllegalArgumentException("Invalid capture rectangle");
        }
    }

    public static CaptureRect between(double x1, double y1, double x2, double y2) {
        return new CaptureRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < right() && py < bottom();
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    public CaptureRect intersect(CaptureRect other) {
        double left = Math.max(x, other.x);
        double top = Math.max(y, other.y);
        return new CaptureRect(
            left,
            top,
            Math.max(0, Math.min(right(), other.right()) - left),
            Math.max(0, Math.min(bottom(), other.bottom()) - top));
    }

    public CaptureRect union(CaptureRect other) {
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        return between(
            Math.min(x, other.x),
            Math.min(y, other.y),
            Math.max(right(), other.right()),
            Math.max(bottom(), other.bottom()));
    }

    public CaptureRect move(double dx, double dy) {
        return new CaptureRect(x + dx, y + dy, width, height);
    }

    public List<CaptureRect> subtract(CaptureRect other) {
        CaptureRect overlap = intersect(other);
        if (overlap.isEmpty()) return List.of(this);
        List<CaptureRect> pieces = new ArrayList<>(4);
        if (overlap.y() > y) pieces.add(new CaptureRect(x, y, width, overlap.y() - y));
        if (overlap.bottom() < bottom())
            pieces.add(new CaptureRect(x, overlap.bottom(), width, bottom() - overlap.bottom()));
        if (overlap.x() > x) pieces.add(new CaptureRect(x, overlap.y(), overlap.x() - x, overlap.height()));
        if (overlap.right() < right())
            pieces.add(new CaptureRect(overlap.right(), overlap.y(), right() - overlap.right(), overlap.height()));
        return List.copyOf(pieces);
    }

    public CaptureRect clamp(CaptureRect bounds) {
        double w = Math.min(width, bounds.width);
        double h = Math.min(height, bounds.height);
        return new CaptureRect(
            Math.clamp(x, bounds.x, bounds.right() - w),
            Math.clamp(y, bounds.y, bounds.bottom() - h),
            w,
            h);
    }
}
