package com.hfstudio.guidenh.client.capture.api;

public interface CaptureLayerSource extends CaptureSource {

    // Return the isolated GUI pixels without the world or the capture interface.
    CaptureFrame captureGuiLayer(CaptureContext context, CapturePixelReader reader, long timestampNanos);

    default CaptureReadback enqueueGuiLayer(CaptureContext context, CapturePixelReader reader, long timestampNanos,
        long memoryBudget) {
        if ((long) context.viewport()
            .pixelWidth() * context.viewport()
                .pixelHeight()
            * 4 > memoryBudget) throw new IllegalArgumentException("The layer readback exceeds the memory budget");
        return new CaptureImmediateReadback(captureGuiLayer(context, reader, timestampNanos));
    }
}
