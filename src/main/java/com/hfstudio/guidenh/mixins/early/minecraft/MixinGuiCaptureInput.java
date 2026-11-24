package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(Minecraft.class)
public class MixinGuiCaptureInput {

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleInput()V"))
    private void guidenh$routeCaptureInput(GuiScreen screen) {
        CaptureRuntime.instance()
            .routeInput(screen);
    }
}
