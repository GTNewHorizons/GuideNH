package com.hfstudio.guidenh.mixins.late.compat.aspectrecipeindex;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo;

import thaumcraft.api.research.ResearchItem;

@Mixin(value = ResearchInfo.class, remap = false)
public interface AccessorResearchInfo {

    @Accessor("researchItem")
    ResearchItem guidenh$getResearchItem();
}
