package com.hfstudio.guidenh.mixins.late.compat.mui2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.screen.ClientScreenHandler;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(value = ClientScreenHandler.class, remap = false)
public class MixinCaptureMui2Background {

    @Inject(method = "drawDarkBackground", at = @At("HEAD"), cancellable = true)
    private static void guidenh$hideCaptureBackdrop(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideBackdrop()) callback.cancel();
    }
}
