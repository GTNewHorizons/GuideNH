package com.hfstudio.guidenh.client.capture.api;

import net.minecraft.client.gui.GuiScreen;

public record CaptureContext(GuiScreen screen, CaptureViewport viewport, long frameId, float partialTicks) {}
