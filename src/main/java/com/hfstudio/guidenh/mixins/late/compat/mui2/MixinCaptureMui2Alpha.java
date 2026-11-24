package com.hfstudio.guidenh.mixins.late.compat.mui2;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.utils.Platform;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(value = Platform.class, remap = false)
public class MixinCaptureMui2Alpha {

    @Inject(method = "setupDrawTex(Z)V", at = @At("RETURN"))
    private static void guidenh$composeCaptureTexture(boolean withBlend, CallbackInfo callback) {
        if (!CaptureRuntime.instance()
            .capturingGuiLayer()) return;
        // Keep native RGB replacement for opaque textures; accumulate coverage over the existing panel.
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(
            withBlend ? GL11.GL_SRC_ALPHA : GL11.GL_ONE,
            withBlend ? GL11.GL_ONE_MINUS_SRC_ALPHA : GL11.GL_ZERO,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Inject(method = "setupDrawColor", at = @At("RETURN"))
    private static void guidenh$composeCaptureColor(CallbackInfo callback) {
        if (!CaptureRuntime.instance()
            .capturingGuiLayer()) return;
        OpenGlHelper
            .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
}
