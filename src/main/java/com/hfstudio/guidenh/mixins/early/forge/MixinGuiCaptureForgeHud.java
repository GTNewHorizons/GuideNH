package com.hfstudio.guidenh.mixins.early.forge;

import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(GuiIngameForge.class)
public class MixinGuiCaptureForgeHud {

    @Inject(method = "renderGameOverlay", at = @At("HEAD"), cancellable = true)
    private void guidenh$prepareForgeHudCapture(float partialTicks, boolean hasScreen, int mouseX, int mouseY,
        CallbackInfo callback) {
        if (!CaptureRuntime.instance()
            .prepareHud()) callback.cancel();
    }
}
