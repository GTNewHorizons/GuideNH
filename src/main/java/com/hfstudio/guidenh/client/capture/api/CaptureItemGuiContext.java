package com.hfstudio.guidenh.client.capture.api;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public record CaptureItemGuiContext(GuiScreen parent, int slot, ItemStack stack) {

    public CaptureItemGuiContext {
        stack = stack.copy();
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }
}
