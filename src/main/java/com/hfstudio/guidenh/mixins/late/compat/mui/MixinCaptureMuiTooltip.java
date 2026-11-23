package com.hfstudio.guidenh.mixins.late.compat.mui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.modularui.api.drawable.GuiHelper;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(value = GuiHelper.class, remap = false)
public class MixinCaptureMuiTooltip {

    @Inject(
        method = "drawHoveringTextFormatted(Ljava/util/List;Ljava/util/List;Lcom/gtnewhorizons/modularui/api/math/Pos2d;Lcom/gtnewhorizons/modularui/api/math/Size;IFZLcom/gtnewhorizons/modularui/api/math/Alignment;Z)V",
        at = @At("HEAD"),
        cancellable = true)
    private static void guidenh$hideCaptureTooltip(CallbackInfo callback) {
        if (CaptureRuntime.instance()
            .hideTooltips()) callback.cancel();
    }

    @Inject(
        method = "drawHoveringTextFormatted(Ljava/util/List;Ljava/util/List;Lcom/gtnewhorizons/modularui/api/math/Pos2d;Lcom/gtnewhorizons/modularui/api/math/Size;IFZLcom/gtnewhorizons/modularui/api/math/Alignment;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/modularui/api/drawable/TextRenderer;setSimulate(Z)V",
            ordinal = 1))
    private static void guidenh$recordCaptureTooltip(CallbackInfo callback, @Local(name = "tooltipX") int x,
        @Local(name = "tooltipY") int y, @Local(name = "tooltipTextWidth") int width,
        @Local(name = "tooltipHeight") int height) {
        CaptureRuntime.instance()
            .recordTooltip(new CaptureRect(x - 4, y - 4, Math.max(0, width + 8), Math.max(0, height + 8)));
    }
}
