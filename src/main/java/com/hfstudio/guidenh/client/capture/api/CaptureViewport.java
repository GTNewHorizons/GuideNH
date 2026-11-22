package com.hfstudio.guidenh.client.capture.api;

public record CaptureViewport(int logicalWidth, int logicalHeight, int pixelWidth, int pixelHeight) {

    public double scaleX() {
        return (double) pixelWidth / logicalWidth;
    }

    public double scaleY() {
        return (double) pixelHeight / logicalHeight;
    }

    public CaptureRect bounds() {
        return new CaptureRect(0, 0, logicalWidth, logicalHeight);
    }

    public CaptureRect pixels(CaptureRect logical) {
        CaptureRect clipped = logical.intersect(bounds());
        double left = Math.floor(clipped.x() * scaleX());
        double top = Math.floor(clipped.y() * scaleY());
        double right = Math.min(pixelWidth, Math.ceil(clipped.right() * scaleX()));
        double bottom = Math.min(pixelHeight, Math.ceil(clipped.bottom() * scaleY()));
        return new CaptureRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}
