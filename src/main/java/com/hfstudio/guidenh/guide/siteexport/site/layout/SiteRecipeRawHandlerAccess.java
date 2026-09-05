package com.hfstudio.guidenh.guide.siteexport.site.layout;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeTagRenderer.HandlerRuntime;

/**
 * Narrow interface for reading NEI handler slots without depending on
 * {@link HandlerRuntime}.
 */
public interface SiteRecipeRawHandlerAccess {

    List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex);

    List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex);

    @Nullable
    NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex);
}
