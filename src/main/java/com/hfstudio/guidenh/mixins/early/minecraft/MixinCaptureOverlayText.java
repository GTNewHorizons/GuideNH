package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(FontRenderer.class)
public class MixinCaptureOverlayText {

    @Inject(method = "renderString", at = @At("HEAD"))
    private void guidenh$beginCapturedString(String text, int x, int y, int color, boolean shadow,
        CallbackInfoReturnable<Integer> callback) {
        CaptureRuntime.instance()
            .beginOverlayText((FontRenderer) (Object) this, text, x, y, color, shadow);
    }

    @Inject(method = "renderString", at = @At("RETURN"))
    private void guidenh$endCapturedString(String text, int x, int y, int color, boolean shadow,
        CallbackInfoReturnable<Integer> callback) {
        CaptureRuntime.instance()
            .endOverlayText();
    }

    @Inject(method = "drawString(Ljava/lang/String;IIIZ)I", at = @At("RETURN"))
    private void guidenh$recordOverlayText(String text, int x, int y, int color, boolean shadow,
        CallbackInfoReturnable<Integer> callback) {
        CaptureRuntime.instance()
            .recordOverlayText(text, x, y, shadow, (FontRenderer) (Object) this);
    }
}
