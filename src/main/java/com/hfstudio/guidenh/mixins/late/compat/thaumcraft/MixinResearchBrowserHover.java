package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
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
    @Unique
    private String guidenh$guideHint;
    @Unique
    private int guidenh$guideHintX;
    @Unique
    private int guidenh$guideHintY;

    @Inject(method = "genResearchBackground", at = @At("HEAD"))
    private void guidenh$resetHint(int x, int y, float ticks, CallbackInfo callback) {
        guidenh$guideHint = null;
    }

    @ModifyArgs(
        method = "genResearchBackground",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/client/gui/GuiResearchBrowser;drawGradientRect(IIIIII)V",
            remap = true),
        require = 2)
    private void guidenh$extendTooltip(Args args) {
        if (currentHighlight == null) return;
        guidenh$guideHint = OpenGuideHotkey.bindingHint(new BindingKey("thaumcraft", currentHighlight.key));
        if (guidenh$guideHint == null) return;
        guidenh$guideHintX = (Integer) args.get(0) + 3;
        guidenh$guideHintY = args.get(3);
        args.set(
            2,
            Math.max(
                (Integer) args.get(2),
                guidenh$guideHintX + fontRendererObj.getStringWidth(guidenh$guideHint) + 3));
        args.set(3, guidenh$guideHintY + fontRendererObj.FONT_HEIGHT + 3);
    }

    @Inject(method = "genResearchBackground", at = @At("TAIL"))
    private void guidenh$drawHint(int x, int y, float ticks, CallbackInfo callback) {
        if (guidenh$guideHint != null) {
            fontRendererObj.drawStringWithShadow(guidenh$guideHint, guidenh$guideHintX, guidenh$guideHintY, -1);
        }
    }

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
