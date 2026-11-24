package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(GuiMainMenu.class)
public class MixinGuiCaptureMainMenu extends GuiScreen {

    @Inject(method = "renderSkybox", at = @At("HEAD"), cancellable = true)
    private void guidenh$hideCapturePanorama(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideBackdrop()) callback.cancel();
    }

    @Redirect(
        method = "drawScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;drawGradientRect(IIIIII)V"))
    private void guidenh$hideCaptureTint(GuiMainMenu menu, int left, int top, int right, int bottom, int start,
        int end) {
        if (!CaptureRuntime.instance()
            .hideBackdrop()) drawGradientRect(left, top, right, bottom, start, end);
    }
}
