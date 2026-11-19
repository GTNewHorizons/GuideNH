package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.integration.thaumcraft.ResearchBrowserHover;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public class MixinResearchBrowserHover extends GuiScreen implements ResearchBrowserHover {

    @Shadow
    private ResearchItem currentHighlight;
    @Unique
    private int guidenh$hoverX;
    @Unique
    private int guidenh$hoverY;

    @Inject(method = "drawScreen", at = @At("TAIL"), remap = true)
    private void guidenh$recordHover(int x, int y, float ticks, CallbackInfo callback) {
        guidenh$hoverX = x;
        guidenh$hoverY = y;
    }

    @Override
    public ResearchItem guidenh$researchAt(int x, int y) {
        return x == guidenh$hoverX && y == guidenh$hoverY ? currentHighlight : null;
    }
}
