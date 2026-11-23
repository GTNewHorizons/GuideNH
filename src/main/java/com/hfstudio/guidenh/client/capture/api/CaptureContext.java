package com.hfstudio.guidenh.client.capture.api;

import net.minecraft.client.gui.GuiScreen;

public record CaptureContext(GuiScreen screen, CaptureViewport viewport, long frameId, float partialTicks,
    CaptureRenderPhase phase, CaptureRenderState renderState) {

    public CaptureContext(GuiScreen screen, CaptureViewport viewport, long frameId, float partialTicks) {
        this(screen, viewport, frameId, partialTicks, null, null);
    }

    public CaptureContext atPhase(CaptureRenderPhase current, CaptureRenderState state) {
        return new CaptureContext(screen, viewport, frameId, partialTicks, current, state);
    }
}
