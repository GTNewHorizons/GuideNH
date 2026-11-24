package com.hfstudio.guidenh.mixins.late.compat.nei;

import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.nei.TextField;

@Mixin(value = TextField.class, remap = false)
public interface AccessorCaptureNeiText {

    @Accessor("field")
    GuiTextField guidenh$getCaptureTextInput();
}
