package com.hfstudio.guidenh.client.capture.api;

public record CaptureViewport(int logicalWidth, int logicalHeight, int pixelWidth, int pixelHeight, double scaleX,
    double scaleY) {

    public CaptureViewport {
        if (logicalWidth < 1 || logicalHeight < 1
            || pixelWidth < 1
            || pixelHeight < 1
            || !Double.isFinite(scaleX)
            || !Double.isFinite(scaleY)
            || scaleX <= 0
            || scaleY <= 0
            || !Double.isFinite(pixelWidth / scaleX)
            || !Double.isFinite(pixelHeight / scaleY))
            throw new IllegalArgumentException(
                "Capture viewports require positive dimensions and finite positive scales");
    }

    public CaptureViewport(int logicalWidth, int logicalHeight, int pixelWidth, int pixelHeight) {
        this(
            logicalWidth,
            logicalHeight,
            pixelWidth,
            pixelHeight,
            (double) pixelWidth / logicalWidth,
            (double) pixelHeight / logicalHeight);
    }

    public CaptureRect bounds() {
        return new CaptureRect(0, 0, pixelWidth / scaleX, pixelHeight / scaleY);
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
