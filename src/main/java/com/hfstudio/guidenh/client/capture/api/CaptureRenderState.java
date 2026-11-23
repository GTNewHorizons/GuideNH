package com.hfstudio.guidenh.client.capture.api;

public record CaptureRenderState(String backendId, boolean openGl, boolean worldRendered, boolean shadowPass,
    int drawFramebuffer, int readFramebuffer, int drawBuffer, int readBuffer, CaptureRect viewportPixels,
    boolean scissorEnabled, CaptureRect scissorPixels, CaptureCapabilities capabilities) {}
