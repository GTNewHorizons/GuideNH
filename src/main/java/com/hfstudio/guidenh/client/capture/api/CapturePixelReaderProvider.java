package com.hfstudio.guidenh.client.capture.api;

public interface CapturePixelReaderProvider {

    String id();

    CaptureProviderAvailability availability(CaptureContext context);

    CapturePixelReader open(CaptureContext context);

    // A context-independent reader can keep its resources for the entire capture session.
    default Object contextKey(CaptureContext context) {
        return null;
    }
}
