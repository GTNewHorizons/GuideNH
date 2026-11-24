package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.GuiResearchRecipe;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public interface AccessorGuiResearchRecipe {

    @Accessor("research")
    ResearchItem guidenh$getResearch();

    @Accessor("page")
    int guidenh$getCapturePage();

    @Accessor("maxPages")
    int guidenh$getCapturePageCount();

    @Accessor("pages")
    ResearchPage[] guidenh$getCapturePages();

    @Accessor("guiMapX")
    double guidenh$getCaptureMapX();

    @Accessor("guiMapY")
    double guidenh$getCaptureMapY();
}
