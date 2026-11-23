package com.hfstudio.guidenh.client.capture.api;

public interface CapturePixelReader extends AutoCloseable {

    CaptureFrame read(int x, int y, int width, int height, long timestampNanos, CaptureAlphaMode alphaMode);

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
