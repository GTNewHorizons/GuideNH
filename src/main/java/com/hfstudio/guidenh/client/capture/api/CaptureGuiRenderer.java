package com.hfstudio.guidenh.client.capture.api;

public interface CaptureGuiRenderer {

    String id();

    boolean supports(CaptureContext context);

    default boolean supportsContent(CaptureContext context, CaptureContentOptions content) {
        return supports(context) && content.includeGui() && !content.includeHud();
    }

    CaptureRenderSnapshot snapshot(CaptureContext context, int mouseX, int mouseY, boolean includeBackdrop);

    default CaptureRenderSnapshot snapshot(CaptureContext context, CaptureContentOptions content, int mouseX,
        int mouseY) {
        return snapshot(context, mouseX, mouseY, content.includeGuiBackdrop());
    }
}
