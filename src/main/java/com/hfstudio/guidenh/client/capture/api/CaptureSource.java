package com.hfstudio.guidenh.client.capture.api;

public interface CaptureSource extends AutoCloseable {

    default void memoryAccount(CaptureMemoryAccount account) {}

    void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry);

    void finishGui(CaptureContext context);

    boolean drawing();

    default boolean drawingOnFramebuffer(int framebuffer) {
        return drawing();
    }

    boolean bindMainFramebuffer(boolean viewport);

    CaptureProviderAvailability readiness(CaptureContext context);

    default CaptureRenderPhase capturePhase() {
        return CaptureRenderPhase.FRAME_READY;
    }

    default CaptureFrame capture(CaptureContext context, CapturePixelReader reader, long timestampNanos) {
        return captureCompletedFrame(context, reader, timestampNanos);
    }

    // Return viewport-sized, top-down straight ARGB pixels at the source's declared capture phase.
    CaptureFrame captureCompletedFrame(CaptureContext context, CapturePixelReader reader, long timestampNanos);

    boolean supportsRedraw();

    @Override
    void close();
}
