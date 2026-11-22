package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.shader.Framebuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(Framebuffer.class)
public class MixinGuiCaptureFramebuffer {

    @Inject(method = "bindFramebuffer", at = @At("HEAD"), cancellable = true)
    private void guidenh$bindCaptureFramebuffer(boolean viewport, CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .bindMainFramebuffer((Framebuffer) (Object) this, viewport)) callback.cancel();
    }

    @Inject(method = "unbindFramebuffer", at = @At("HEAD"), cancellable = true)
    private void guidenh$keepCaptureFramebuffer(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .bindMainFramebuffer((Framebuffer) (Object) this, false)) callback.cancel();
    }
}
