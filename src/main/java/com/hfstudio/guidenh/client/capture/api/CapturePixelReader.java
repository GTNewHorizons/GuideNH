package com.hfstudio.guidenh.client.capture.api;

public interface CapturePixelReader extends AutoCloseable {

    CaptureFrame read(int x, int y, int width, int height, long timestampNanos, CaptureAlphaMode alphaMode);

    default CaptureReadback enqueue(int x, int y, int width, int height, long timestampNanos,
        CaptureAlphaMode alphaMode, long memoryBudget) {
        if (width <= 0 || height <= 0 || (long) width * height * 4 > memoryBudget)
            throw new IllegalArgumentException("The readback dimensions exceed the available memory budget");
        return new CaptureImmediateReadback(read(x, y, width, height, timestampNanos, alphaMode));
    }

    default CaptureFrame read(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, CaptureAlphaMode.STRAIGHT);
    }

    default CaptureFrame readPremultiplied(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, CaptureAlphaMode.PREMULTIPLIED);
    }

    default CaptureFrame readOpaque(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, CaptureAlphaMode.OPAQUE);
    }

    @Override
    void close();
}
