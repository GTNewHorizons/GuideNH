package com.hfstudio.guidenh.mixins.early.compat.lwjgl3ify;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.integration.lwjgl3ify.capture.LwjglCaptureInputBridge;

import me.eigenraven.lwjgl3ify.api.InputEvents;

@Mixin(value = InputEvents.class, remap = false)
public class MixinCaptureInputEvents {

    @Inject(method = "injectKeyEvent", at = @At("HEAD"), cancellable = true)
    private static void guidenh$routeCaptureKey(InputEvents.KeyEvent event, CallbackInfo callback) {
        if (LwjglCaptureInputBridge.key(event)) callback.cancel();
    }

    @Inject(method = "injectTextEvent", at = @At("HEAD"), cancellable = true)
    private static void guidenh$routeCaptureText(InputEvents.TextEvent event, CallbackInfo callback) {
        if (LwjglCaptureInputBridge.text(event)) callback.cancel();
    }
}
