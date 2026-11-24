package com.hfstudio.guidenh.mixins.late.compat.aspectrecipeindex;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler$CachedThaumRecipe", remap = false)
public interface AccessorCachedThaumRecipe {

    @Accessor("prereqs")
    List<ResearchInfo> guidenh$getResearchInfo();
}
