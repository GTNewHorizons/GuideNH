package com.hfstudio.guidenh.client.capture.api;

public interface CaptureGeometryProvider {

    String id();

    boolean supports(CaptureContext context);

    default boolean ownsContainerSlots() {
        return false;
    }

    default CaptureGeometry collect(CaptureContext context, boolean includeContainerSlots) {
        return collect(context);
    }

    CaptureGeometry collect(CaptureContext context);
}
