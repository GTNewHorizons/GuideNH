package com.hfstudio.guidenh.client.capture.api;

import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public record CaptureItemGuiFactory(String id, String label, Predicate<ItemStack> supports,
    Function<CaptureItemGuiContext, GuiScreen> create) {}
