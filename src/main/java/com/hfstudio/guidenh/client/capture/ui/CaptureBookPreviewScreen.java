package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.mixins.early.minecraft.AccessorCaptureBookScreen;

public class CaptureBookPreviewScreen extends GuiScreenBook {

    private final ItemStack snapshot;

    public CaptureBookPreviewScreen(ItemStack stack) {
        super(Minecraft.getMinecraft().thePlayer, stack.copy(), false);
        snapshot = stack.copy();
    }

    public ItemStack snapshot() {
        return snapshot.copy();
    }

    public int page() {
        return ((AccessorCaptureBookScreen) this).guidenh$capturePage();
    }

    public int pageCount() {
        return ((AccessorCaptureBookScreen) this).guidenh$capturePageCount();
    }

    public void page(int page) {
        AccessorCaptureBookScreen access = (AccessorCaptureBookScreen) this;
        access.guidenh$capturePage(Math.clamp(page, 0, Math.max(0, pageCount() - 1)));
        access.guidenh$updateCaptureBookButtons();
    }
}
