package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;

@Mixin(value = GuiScreenCanvas.class, remap = false)
public interface AccessorCaptureBqCanvas {

    @Accessor("popup")
    IGuiPanel guidenh$getCapturePopup();

    @Accessor("previousScreen")
    GuiScreen guidenh$getCapturePreviousScreen();
}
