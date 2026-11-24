package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FontRenderer.class)
public interface AccessorCaptureFontStrings {

    @Invoker("renderString")
    int guidenh$renderCapturedString(String text, int x, int y, int color, boolean shadow);

    @Invoker("resetStyles")
    void guidenh$resetCapturedStringStyles();
}
