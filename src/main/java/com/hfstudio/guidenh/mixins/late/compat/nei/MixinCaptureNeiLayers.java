package com.hfstudio.guidenh.mixins.late.compat.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

import codechicken.nei.LayoutManager;

@Mixin(value = LayoutManager.class, remap = false)
public class MixinCaptureNeiLayers {

    @Inject(method = { "renderObjects", "postRenderObjects" }, at = @At("HEAD"), cancellable = true)
    private void guidenh$hideCaptureSidebar(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideNei()) callback.cancel();
    }
}
