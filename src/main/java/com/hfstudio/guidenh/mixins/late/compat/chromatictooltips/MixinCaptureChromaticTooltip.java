package com.hfstudio.guidenh.mixins.late.compat.chromatictooltips;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.slprime.chromatictooltips.TooltipRenderer;
import com.slprime.chromatictooltips.event.RenderTooltipEvent;

import cpw.mods.fml.common.eventhandler.Event;

@Mixin(value = TooltipRenderer.class, remap = false)
public class MixinCaptureChromaticTooltip {

    @ModifyArg(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/slprime/chromatictooltips/util/TooltipUtils;postEvent(Lcpw/mods/fml/common/eventhandler/Event;)Z"))
    private Event guidenh$recordRenderedTooltip(Event event) {
        if (event instanceof RenderTooltipEvent rendered) CaptureRuntime.instance()
            .recordTooltip(new CaptureRect(rendered.x, rendered.y, rendered.width, rendered.height));
        return event;
    }
}
