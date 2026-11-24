package com.hfstudio.guidenh.client.capture.api;

public record CaptureScrollGeometry(String groupId, CaptureRect viewport, double offsetX, double offsetY,
    double contentWidth, double contentHeight) {

    public CaptureScrollGeometry {
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY)
            || !Double.isFinite(contentWidth)
            || !Double.isFinite(contentHeight)
            || offsetX < 0
            || offsetY < 0
            || contentWidth <= 0
            || contentHeight <= 0) throw new IllegalArgumentException("Invalid scroll capture geometry");
    }
}
