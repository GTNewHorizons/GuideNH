package com.hfstudio.guidenh.mixins.early.compat.angelica;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.hudcaching.HUDCaching;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(value = HUDCaching.class, remap = false)
public class MixinCaptureHudCache {

    @ModifyExpressionValue(
        method = "renderCachedHud",
        at = @At(value = "FIELD", target = "Lcom/gtnewhorizons/angelica/config/AngelicaConfig;hudCachingActive:Z"),
        require = 0)
    private static boolean guidenh$drawCurrentCaptureHud(boolean cached) {
        // Keep the normal HUD events while avoiding pixels cached under different capture content settings.
        return cached && !CaptureRuntime.instance()
            .capturingGuiLayer();
    }

    @Inject(method = "renderCachedHud", at = @At("HEAD"), cancellable = true, require = 0)
    private static void guidenh$prepareCachedHudCapture(EntityRenderer renderer, GuiIngame ingame, float partialTicks,
        boolean hasScreen, int mouseX, int mouseY, CallbackInfo callback) {
        if (!CaptureRuntime.instance()
            .prepareHud()) callback.cancel();
    }
}
