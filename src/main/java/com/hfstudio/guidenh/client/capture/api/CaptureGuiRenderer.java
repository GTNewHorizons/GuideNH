package com.hfstudio.guidenh.client.capture.api;

public interface CaptureGuiRenderer {

    String id();

    boolean supports(CaptureContext context);

    CaptureRenderSnapshot snapshot(CaptureContext context, int mouseX, int mouseY, boolean includeBackdrop);
}
