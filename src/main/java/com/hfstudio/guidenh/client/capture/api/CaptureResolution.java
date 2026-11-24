package com.hfstudio.guidenh.client.capture.api;

public record CaptureResolution(double scaleX, double scaleY, int requestedWidth, int requestedHeight,
    boolean keepAspectRatio, String qualityPolicy) {

    public static final int MAX_SCALE = 32;

    public CaptureResolution {
        if (!Double.isFinite(scaleX) || !Double.isFinite(scaleY)
            || scaleX <= 0
            || scaleY <= 0
            || scaleX > MAX_SCALE
            || scaleY > MAX_SCALE
            || requestedWidth < 0
            || requestedHeight < 0) {
            throw new IllegalArgumentException("Capture scale must be positive and at most 32");
        }
    }

    public CaptureOutputSize resolve(int baseWidth, int baseHeight) {
        long width = requestedWidth > 0 ? requestedWidth : Math.round(baseWidth * scaleX);
        long height = requestedHeight > 0 ? requestedHeight : Math.round(baseHeight * scaleY);
        if (keepAspectRatio && requestedWidth > 0 && requestedHeight == 0) {
            height = Math.round((double) baseHeight * width / baseWidth);
        } else if (keepAspectRatio && requestedHeight > 0 && requestedWidth == 0) {
            width = Math.round((double) baseWidth * height / baseHeight);
        }
        if (width > (long) baseWidth * MAX_SCALE || height > (long) baseHeight * MAX_SCALE) {
            throw new IllegalArgumentException("Output dimensions exceed 32 times the selected area");
        }
        return new CaptureOutputSize(Math.toIntExact(width), Math.toIntExact(height));
    }
}
