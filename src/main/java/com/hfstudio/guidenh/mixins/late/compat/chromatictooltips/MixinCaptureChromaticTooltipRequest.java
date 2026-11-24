package com.hfstudio.guidenh.mixins.late.compat.chromatictooltips;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.slprime.chromatictooltips.TooltipHandler;

@Mixin(value = TooltipHandler.class, remap = false)
public class MixinCaptureChromaticTooltipRequest {

    @Inject(
        method = "drawHoveringText(Lcom/slprime/chromatictooltips/api/TooltipRequest;)V",
        at = @At("HEAD"),
        cancellable = true)
    private static void guidenh$hideTooltipRequest(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }

    @ModifyArgs(
        method = "drawLastTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lcom/slprime/chromatictooltips/api/TooltipContext;drawAtMousePosition(II)V"))
    private static void guidenh$freezeTooltipPointer(Args arguments) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        arguments.set(0, runtime.captureMouseX(arguments.get(0)));
        arguments.set(1, runtime.captureMouseY(arguments.get(1)));
    }
}
