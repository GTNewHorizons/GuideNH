package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureLayerSource;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;

public class FinalFrameCaptureSource implements CaptureLayerSource {

    private final GuiLayerCaptureSource guiLayer = new GuiLayerCaptureSource();
    private long completedFrame = Long.MIN_VALUE;

    @Override
    public void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry) {
        completedFrame = Long.MIN_VALUE;
        guiLayer.beginGui(context, content, collectGeometry);
    }

    @Override
    public void finishGui(CaptureContext context) {
        guiLayer.finishGui(context);
        completedFrame = context.frameId();
    }

    @Override
    public boolean drawing() {
        return guiLayer.drawing();
    }

    @Override
    public boolean bindMainFramebuffer(boolean viewport) {
        return guiLayer.bindMainFramebuffer(viewport);
    }

    @Override
    public CaptureProviderAvailability readiness(CaptureContext context) {
        return completedFrame == context.frameId() ? guiLayer.readiness(context)
            : CaptureProviderAvailability.unavailable("The final GUI frame has not completed");
    }

    @Override
    public CaptureFrame captureCompletedFrame(CaptureContext context, CapturePixelReader reader, long timestamp) {
        return guiLayer.captureCompletedFrame(context, reader, timestamp);
    }

    @Override
    public CaptureFrame captureGuiLayer(CaptureContext context, CapturePixelReader reader, long timestamp) {
        return guiLayer.captureGuiLayer(context, reader, timestamp);
    }

    @Override
    public boolean supportsRedraw() {
        return guiLayer.supportsRedraw();
    }

    @Override
    public void close() {
        guiLayer.close();
        completedFrame = Long.MIN_VALUE;
    }
}
