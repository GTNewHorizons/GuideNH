package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.thaumcraft.ThaumcraftResearchHoverProvider;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchRecipe;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public class MixinResearchRecipeGuide extends GuiScreen {

    @Shadow
    private ResearchItem research;

    @Inject(method = "drawScreen", at = @At("TAIL"), remap = true)
    private void guidenh$drawGuideHint(int x, int y, float ticks, CallbackInfo callback) {
        if (!ThaumcraftResearchHoverProvider.isResearchTitleHovered(this, x, y)) return;
        String hint = OpenGuideHotkey.bindingHint(new BindingKey("thaumcraft", research.key));
        if (hint != null) {
            fontRendererObj.drawStringWithShadow(
                hint,
                (width - fontRendererObj.getStringWidth(hint)) / 2,
                Math.min(height - fontRendererObj.FONT_HEIGHT - 4, (height + 181) / 2 + 8),
                -1);
        }
    }
}
