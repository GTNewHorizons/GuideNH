package com.hfstudio.guidenh.mixins.late.compat.mui2;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

@Mixin(value = GuiDraw.class, remap = false)
public class MixinCaptureMui2Tooltip {

    @Inject(method = "drawTooltipBackground", at = @At("HEAD"))
    private static void guidenh$recordCaptureTooltip(ItemStack stack, List<String> lines, int x, int y, int width,
        int height, RichTooltip tooltip, CallbackInfo callback) {
        CaptureRuntime.instance()
            .recordTooltip(new CaptureRect(x - 4, y - 4, Math.max(0, width + 8), Math.max(0, height + 8)));
    }
}
