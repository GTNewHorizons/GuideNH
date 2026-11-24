package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;

public class CaptureRecordedGuiRenderer implements CaptureGuiRenderer {

    private final CaptureFrozenLayers layers;

    public CaptureRecordedGuiRenderer(CaptureFrozenLayers layers) {
        this.layers = layers;
    }

    @Override
    public String id() {
        return "recorded-draws";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return layers.snapshot(context) != null;
    }

    @Override
    public boolean supportsContent(CaptureContext context, CaptureContentOptions content) {
        return layers.snapshot(context, content) != null;
    }

    @Override
    public CaptureRenderSnapshot snapshot(CaptureContext context, int mouseX, int mouseY, boolean includeBackdrop) {
        CaptureRecordedSnapshot snapshot = layers.snapshot(context);
        if (snapshot == null) throw new IllegalStateException("The recorded GUI snapshot is unavailable");
        return snapshot.retain();
    }

    @Override
    public CaptureRenderSnapshot snapshot(CaptureContext context, CaptureContentOptions content, int mouseX,
        int mouseY) {
        CaptureRecordedSnapshot snapshot = layers.snapshot(context, content);
        if (snapshot == null) throw new IllegalStateException("The recorded GUI snapshot is unavailable");
        return snapshot.retain();
    }
}
