package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.GuiScreenBook;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiScreenBook.class)
public interface AccessorCaptureBookScreen {

    @Accessor("currPage")
    int guidenh$capturePage();

    @Accessor("currPage")
    void guidenh$capturePage(int page);

    @Accessor("bookTotalPages")
    int guidenh$capturePageCount();

    @Invoker("updateButtons")
    void guidenh$updateCaptureBookButtons();
}
