package com.hfstudio.guidenh.client.capture.api;

public record CaptureOutputSize(int width, int height) {

    public CaptureOutputSize {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Output dimensions must be positive");
    }

    public long rgbaBytes() {
        try {
            return Math.multiplyExact(Math.multiplyExact((long) width, height), 4);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Capture dimensions exceed the supported RGBA byte count", error);
        }
    }
}
