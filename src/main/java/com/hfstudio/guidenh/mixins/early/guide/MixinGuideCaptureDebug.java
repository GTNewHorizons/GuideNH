package com.hfstudio.guidenh.mixins.early.guide;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.guide.internal.debug.GuideDebugOverlay;

@Mixin(GuideDebugOverlay.class)
public class MixinGuideCaptureDebug {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void guidenh$hideCaptureDebug(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideDebug()) callback.cancel();
    }
}
