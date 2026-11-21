package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchRecipe;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public interface AccessorGuiResearchRecipe {

    @Accessor("research")
    ResearchItem guidenh$getResearch();
}
