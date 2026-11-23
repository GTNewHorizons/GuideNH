package com.hfstudio.guidenh.client.capture.api;

public interface CapturePixelReaderProvider {

    String id();

    CaptureProviderAvailability availability(CaptureContext context);

    CapturePixelReader open(CaptureContext context);
}
