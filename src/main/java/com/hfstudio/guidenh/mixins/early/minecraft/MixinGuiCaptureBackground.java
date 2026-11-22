package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(GuiScreen.class)
public class MixinGuiCaptureBackground {

    @Inject(method = { "drawWorldBackground", "drawBackground" }, at = @At("HEAD"), cancellable = true)
    private void guidenh$hideCaptureBackdrop(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideBackdrop()) callback.cancel();
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    private void guidenh$hideCaptureTooltip(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }

    @Inject(
        method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void guidenh$hideCaptureHoverText(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }
}
