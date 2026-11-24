package com.hfstudio.guidenh.client.capture.api;

public record CaptureOutputSize(int width, int height) {

    public CaptureOutputSize {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Output dimensions must be positive");
    }

    public long rgbaBytes() {
        return (long) width * height * 4;
    }
}
