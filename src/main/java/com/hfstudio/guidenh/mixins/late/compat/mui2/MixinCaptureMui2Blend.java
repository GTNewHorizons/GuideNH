package com.hfstudio.guidenh.mixins.late.compat.mui2;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.utils.GlStateManager;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.render.CaptureAlphaBlend;

@Mixin(value = GlStateManager.class, remap = false)
public class MixinCaptureMui2Blend {

    @Inject(method = "blendFunc", at = @At("HEAD"), cancellable = true)
    private static void guidenh$preserveCaptureAlpha(int source, int destination, CallbackInfo callback) {
        if (!CaptureRuntime.instance()
            .capturingGuiLayer() || !CaptureAlphaBlend.sourceOver(source, destination)) return;
        OpenGlHelper.glBlendFunc(source, destination, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        callback.cancel();
    }
}
