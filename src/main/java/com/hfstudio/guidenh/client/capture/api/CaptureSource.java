package com.hfstudio.guidenh.client.capture.api;

public interface CaptureSource extends AutoCloseable {

    void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry);

    void finishGui(CaptureContext context);

    boolean drawing();

    boolean bindMainFramebuffer(boolean viewport);

    CaptureProviderAvailability readiness(CaptureContext context);

    // Read at the completed-frame callback; return viewport-sized, top-down straight ARGB pixels.
    CaptureFrame captureCompletedFrame(CaptureContext context, CapturePixelReader reader, long timestampNanos);

    boolean supportsRedraw();

    @Override
    void close();
}
