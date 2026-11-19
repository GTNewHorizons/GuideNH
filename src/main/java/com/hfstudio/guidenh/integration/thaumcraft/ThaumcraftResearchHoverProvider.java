package com.hfstudio.guidenh.integration.thaumcraft;

import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.integration.api.binding.BindingHoverTarget;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.api.binding.GuideBindingHoverProvider;
import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchRecipe;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;

public class ThaumcraftResearchHoverProvider implements GuideBindingHoverProvider {

    @Override
    @Nullable
    public BindingHoverTarget findHoveredBinding(GuiScreen screen, int x, int y) {
        ResearchItem item = null;
        if (screen instanceof GuiResearchBrowser browser)
            item = ((ResearchBrowserHover) browser).guidenh$researchAt(x, y);
        else if (screen instanceof GuiResearchRecipe recipe && isResearchTitleHovered(screen, x, y))
            item = ((AccessorGuiResearchRecipe) recipe).guidenh$getResearch();
        return item == null ? null
            : new BindingHoverTarget(
                new BindingKey("thaumcraft", item.key),
                new LytRect(0, 0, screen.width, screen.height));
    }

    public static boolean isResearchTitleHovered(GuiScreen screen, int x, int y) {
        int top = (screen.height - 181) / 2;
        return x >= (screen.width - 256) / 2 && x < (screen.width + 256) / 2 && y >= top - 24 && y < top + 16;
    }
}
