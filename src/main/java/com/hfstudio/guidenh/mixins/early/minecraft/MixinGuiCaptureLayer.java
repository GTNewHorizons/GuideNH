package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(EntityRenderer.class)
public class MixinGuiCaptureLayer {

    @Inject(
        method = "updateCameraAndRender",
        at = @At(
            value = "NEW",
            target = "net/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Pre",
            remap = false))
    private void guidenh$beginCaptureGui(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .beginGui(partialTicks);
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void guidenh$finishCaptureGui(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .finishGui();
    }
}
