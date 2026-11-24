package com.hfstudio.guidenh.client.capture.api;

public interface CaptureLayerSource extends CaptureSource {

    // Return the isolated GUI pixels without the world or the capture interface.
    CaptureFrame captureGuiLayer(CaptureContext context, CapturePixelReader reader, long timestampNanos);
}
