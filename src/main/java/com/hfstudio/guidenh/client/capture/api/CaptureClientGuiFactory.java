package com.hfstudio.guidenh.client.capture.api;

import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiScreen;

public record CaptureClientGuiFactory(String id, String label, Class<? extends GuiScreen> screenType,
    Predicate<CaptureBatchContext> available, String unavailableReason, Function<GuiScreen, GuiScreen> create) {}
