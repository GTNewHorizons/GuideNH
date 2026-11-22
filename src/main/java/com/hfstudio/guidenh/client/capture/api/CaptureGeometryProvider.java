package com.hfstudio.guidenh.client.capture.api;

public interface CaptureGeometryProvider {

    String id();

    boolean supports(CaptureContext context);

    CaptureGeometry collect(CaptureContext context);
}
