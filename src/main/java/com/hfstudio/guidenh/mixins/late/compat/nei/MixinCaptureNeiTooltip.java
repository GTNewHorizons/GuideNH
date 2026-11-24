package com.hfstudio.guidenh.mixins.late.compat.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

import codechicken.lib.gui.GuiDraw;

@Mixin(value = GuiDraw.class, remap = false)
public class MixinCaptureNeiTooltip {

    @Inject(
        method = "drawMultilineTip(Lnet/minecraft/client/gui/FontRenderer;IILjava/util/List;IIII)V",
        at = @At("HEAD"),
        cancellable = true)
    private static void guidenh$hideCaptureTooltip(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }

    @Inject(method = "drawTooltipBox(IIIIIIII)V", at = @At("HEAD"))
    private static void guidenh$recordCaptureTooltip(int x, int y, int width, int height, int backgroundStart,
        int backgroundEnd, int borderStart, int borderEnd, CallbackInfo callback) {
        CaptureRuntime.instance()
            .recordTooltip(new CaptureRect(x, y, Math.max(0, width + 1), Math.max(0, height + 1)));
    }
}
