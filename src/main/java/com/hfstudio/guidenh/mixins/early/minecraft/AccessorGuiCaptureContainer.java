package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface AccessorGuiCaptureContainer {

    @Accessor("guiLeft")
    int guidenh$getCaptureGuiLeft();

    @Accessor("guiTop")
    int guidenh$getCaptureGuiTop();

    @Accessor("xSize")
    int guidenh$getCaptureGuiWidth();

    @Accessor("ySize")
    int guidenh$getCaptureGuiHeight();
}
