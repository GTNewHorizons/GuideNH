package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.GuiIngame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(GuiIngame.class)
public class MixinGuiCaptureHud {

    @Inject(method = "renderGameOverlay", at = @At("HEAD"), cancellable = true)
    private void guidenh$prepareHudCapture(float partialTicks, boolean hasScreen, int mouseX, int mouseY,
        CallbackInfo callback) {
        if (!CaptureRuntime.instance().prepareHud()) callback.cancel();
    }
}
