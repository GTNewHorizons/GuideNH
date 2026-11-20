package com.hfstudio.guidenh.mixins.late.compat.aspectrecipeindex;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

@Mixin(value = ResearchInfo.class, remap = false)
public class MixinAriResearchTooltip {

    @Inject(method = "onHover", at = @At("TAIL"))
    private void guidenh$guideHint(List<String> lines, CallbackInfo callback) {
        var item = ((AccessorResearchInfo) this).guidenh$getResearchItem();
        if (item != null && item.key != null)
            OpenGuideHotkey.appendBindingTooltip(new BindingKey("thaumcraft", item.key), lines);
    }
}
