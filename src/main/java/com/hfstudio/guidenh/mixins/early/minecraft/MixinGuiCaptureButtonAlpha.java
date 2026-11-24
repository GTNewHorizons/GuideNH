package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.render.CaptureAlphaBlend;

@Mixin(GuiButton.class)
public class MixinGuiCaptureButtonAlpha {

    @Redirect(
        method = "drawButton",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBlendFunc(II)V", remap = false))
    private void guidenh$blendCaptureButton(int source, int destination) {
        if (CaptureAlphaBlend.sourceOver(source, destination) && CaptureRuntime.instance()
            .capturingGuiLayer()) {
            OpenGlHelper.glBlendFunc(source, destination, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else GL11.glBlendFunc(source, destination);
    }
}
