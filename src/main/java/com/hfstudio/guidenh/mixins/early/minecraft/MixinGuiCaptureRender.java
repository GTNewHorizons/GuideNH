package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(Minecraft.class)
public class MixinGuiCaptureRender {

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V"))
    private void guidenh$finishCaptureFrame(CallbackInfo callback) {
        CaptureRuntime.instance()
            .frameReady();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void guidenh$releaseCaptureResources(CallbackInfo callback) {
        CaptureRuntime.instance()
            .shutdown();
    }
}
