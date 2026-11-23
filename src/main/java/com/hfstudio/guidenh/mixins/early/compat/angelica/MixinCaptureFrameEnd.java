package com.hfstudio.guidenh.mixins.early.compat.angelica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.glsm.hooks.FrameHooks", remap = false)
public class MixinCaptureFrameEnd {

    @Inject(method = "frameEnd", at = @At("HEAD"), require = 0)
    private static void guidenh$captureBeforeBackendSubmit(CallbackInfo callback) {
        CaptureRuntime.instance()
            .frameReady();
    }
}
