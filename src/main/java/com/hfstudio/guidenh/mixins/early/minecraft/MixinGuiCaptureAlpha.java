package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(OpenGlHelper.class)
public class MixinGuiCaptureAlpha {

    @ModifyVariable(method = "glBlendFunc", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private static int guidenh$preserveCaptureAlpha(int destinationAlpha) {
        return destinationAlpha == GL11.GL_ZERO && CaptureRuntime.instance()
            .capturingGuiLayer() ? GL11.GL_ONE_MINUS_SRC_ALPHA : destinationAlpha;
    }
}
