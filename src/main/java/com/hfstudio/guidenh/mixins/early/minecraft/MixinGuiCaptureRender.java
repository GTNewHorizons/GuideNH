package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;

@Mixin(Minecraft.class)
public class MixinGuiCaptureRender {

    @Redirect(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;doesGuiPauseGame()Z"))
    private boolean guidenh$allowServerCaptureTick(GuiScreen screen) {
        return !CaptureClientConnection.instance()
            .needsServerTick() && screen.doesGuiPauseGame();
    }

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V"))
    private void guidenh$finishCaptureFrame(CallbackInfo callback) {
        CaptureRuntime.instance()
            .frameReady();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void guidenh$releaseCaptureResources(CallbackInfo callback) {
        CaptureRuntime.instance()
            .shutdown();
    }
}
