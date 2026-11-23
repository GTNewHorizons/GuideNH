package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.OpenGlHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(OpenGlHelper.class)
public class MixinCaptureGuiBrightness {

    @ModifyVariable(method = "setLightmapTextureCoords", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float guidenh$captureBrightnessX(float value) {
        return CaptureRuntime.instance()
            .fullBrightness() ? 240 : value;
    }

    @ModifyVariable(method = "setLightmapTextureCoords", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static float guidenh$captureBrightnessY(float value) {
        return CaptureRuntime.instance()
            .fullBrightness() ? 240 : value;
    }
}
