package com.hfstudio.guidenh.mixins.late.compat;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.integration.betterquesting.BqGuidePageLinks;

import betterquesting.api2.client.gui.panels.content.PanelTextBox;

@Mixin(value = PanelTextBox.class, remap = false)
public abstract class MixinPanelTextBox {

    @ModifyVariable(
        method = "setText(Ljava/lang/String;)Lbetterquesting/api2/client/gui/panels/content/PanelTextBox;",
        at = @At("HEAD"),
        argsOnly = true)
    private String guidenh$replaceGuideTags(String text) {
        return BqGuidePageLinks.replaceGuideTags(text);
    }

    @Inject(method = "getTooltip(II)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void guidenh$getGuideLinkTooltip(int mx, int my, CallbackInfoReturnable<List<String>> cir) {
        int mxt = mx + ((PanelTextBox) (Object) this).getTransform()
            .getX();
        int myt = my + ((PanelTextBox) (Object) this).getTransform()
            .getY();
        for (Object hotZone : ((AccessorPanelTextBox) this).guidenh$getHotZones()) {
            AccessorPanelTextBoxHotZone accessor = (AccessorPanelTextBoxHotZone) hotZone;
            if (accessor.guidenh$getLocation()
                .contains(mxt, myt)) {
                List<String> tooltip = BqGuidePageLinks.getTooltip(accessor.guidenh$getUrl());
                if (tooltip != null && !tooltip.isEmpty()) {
                    cir.setReturnValue(tooltip);
                }
                return;
            }
        }
    }
}
