package com.hfstudio.guidenh.mixins.late.compat.mui2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.screen.RichTooltip;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(value = RichTooltip.class, remap = false)
public class MixinCaptureMui2TooltipVisibility {

    @Inject(
        method = "draw(Lcom/cleanroommc/modularui/screen/viewport/GuiContext;Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void guidenh$hideCaptureTooltip(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }
}
