package com.hfstudio.guidenh.mixins.early.compat.angelica;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.hudcaching.HUDCaching", remap = false)
public class MixinCaptureHudCache {

    @Inject(method = "renderCachedHud", at = @At("HEAD"), cancellable = true, require = 0)
    private static void guidenh$prepareCachedHudCapture(EntityRenderer renderer, GuiIngame ingame, float partialTicks,
        boolean hasScreen, int mouseX, int mouseY, CallbackInfo callback) {
        if (!CaptureRuntime.instance().prepareHud()) callback.cancel();
    }
}
