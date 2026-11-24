package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(EntityRenderer.class)
public class MixinGuiCaptureLayer {

    @Redirect(
        method = "updateCameraAndRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayInGameMenu()V"))
    private void guidenh$keepGameplayCaptureOnFocusLoss(Minecraft minecraft) {
        // Gameplay capture keeps the real screen unset so HUD visibility conditions remain valid.
        if (!CaptureRuntime.instance()
            .gameplayInputCaptured()) minecraft.displayInGameMenu();
    }

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void guidenh$beginCaptureFrame(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .beginFrame(partialTicks);
    }

    @Inject(
        method = "updateCameraAndRender",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;renderEndNanoTime:J",
            opcode = Opcodes.PUTFIELD))
    private void guidenh$worldCaptureReady(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .worldReady();
    }

    @Inject(
        method = "updateCameraAndRender",
        at = @At(
            value = "NEW",
            target = "net/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Pre",
            remap = false))
    private void guidenh$beginCaptureGui(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .beginGui(partialTicks);
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void guidenh$finishCaptureGui(float partialTicks, CallbackInfo callback) {
        CaptureRuntime.instance()
            .finishGui();
    }

    @ModifyArgs(
        method = "updateCameraAndRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V"))
    private void guidenh$freezeSourcePointer(Args arguments) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        arguments.set(0, runtime.captureMouseX(arguments.get(0)));
        arguments.set(1, runtime.captureMouseY(arguments.get(1)));
    }

    @ModifyArgs(
        method = "updateCameraAndRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Pre;<init>(Lnet/minecraft/client/gui/GuiScreen;IIF)V",
            remap = false))
    private void guidenh$freezeGuiPrePointer(Args arguments) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        arguments.set(1, runtime.captureMouseX(arguments.get(1)));
        arguments.set(2, runtime.captureMouseY(arguments.get(2)));
    }

    @ModifyArgs(
        method = "updateCameraAndRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Post;<init>(Lnet/minecraft/client/gui/GuiScreen;IIF)V",
            remap = false))
    private void guidenh$freezeGuiPostPointer(Args arguments) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        arguments.set(1, runtime.captureMouseX(arguments.get(1)));
        arguments.set(2, runtime.captureMouseY(arguments.get(2)));
    }
}
