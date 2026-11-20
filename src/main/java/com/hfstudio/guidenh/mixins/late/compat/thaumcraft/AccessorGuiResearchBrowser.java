package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public interface AccessorGuiResearchBrowser {

    @Accessor("currentHighlight")
    ResearchItem guidenh$getHighlightedResearch();

    @Accessor("selectedCategory")
    static void guidenh$setSelectedCategory(String category) {
        throw new AssertionError();
    }
}
