package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiScreen.class)
public interface AccessorGuiCaptureScreen {

    @Accessor("buttonList")
    List<GuiButton> guidenh$getCaptureButtons();
}
