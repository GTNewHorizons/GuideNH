package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.OpenGlHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.render.CaptureAlphaBlend;

@Mixin(OpenGlHelper.class)
public class MixinGuiCaptureAlpha {

    @ModifyVariable(method = "glBlendFunc", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private static int guidenh$preserveCaptureAlpha(int destinationAlpha, int source, int destination, int sourceAlpha,
        int ignoredAlpha) {
        return CaptureRuntime.instance()
            .capturingGuiLayer()
                ? CaptureAlphaBlend.destinationAlpha(source, destination, sourceAlpha, destinationAlpha)
                : destinationAlpha;
    }

    @ModifyVariable(method = "glBlendFunc", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private static int guidenh$preserveSourceAlpha(int sourceAlpha, int source, int destination, int ignoredAlpha,
        int destinationAlpha) {
        return CaptureRuntime.instance()
            .capturingGuiLayer() ? CaptureAlphaBlend.sourceAlpha(source, destination, sourceAlpha, destinationAlpha)
                : sourceAlpha;
    }
}
