package com.hfstudio.guidenh.client.capture.api;

public interface CaptureSourceProvider {

    String id();

    CaptureProviderAvailability availability(CaptureContext context, CaptureContentOptions content);

    CaptureSource open(CaptureContext context);

    default boolean automaticSelection(CaptureContext context, CaptureContentOptions content) {
        return true;
    }

    // Include every context or capability generation that owns this source's resources.
    default Object contextKey(CaptureContext context) {
        return context.screen();
    }
}
