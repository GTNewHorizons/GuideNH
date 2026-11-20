package com.hfstudio.guidenh.integration.nei;

import com.hfstudio.guidenh.integration.api.RecipeHandlerRenderProvider;

import codechicken.nei.recipe.IRecipeHandler;

public class NeiRecipeHandlerRenderProvider implements RecipeHandlerRenderProvider {

    @Override
    public boolean canRenderRecipeHandler(Object handler) {
        return handler instanceof IRecipeHandler;
    }

    @Override
    public void renderRecipeHandler(Object handler, int recipeIndex, boolean skipForeground) {
        renderRecipeHandlerBackground(handler, recipeIndex, skipForeground);
        renderRecipeHandlerForeground(handler, recipeIndex, skipForeground);
    }

    @Override
    public void renderRecipeHandlerBackground(Object handler, int recipeIndex, boolean skipForeground) {
        NeiRecipeLookup.callDrawBackground(handler, recipeIndex);
    }

    @Override
    public void renderRecipeHandlerForeground(Object handler, int recipeIndex, boolean skipForeground) {
        NeiDirectCalls.drawRecipeBadges(handler, recipeIndex, skipForeground);
        if (!skipForeground) {
            NeiRecipeLookup.callDrawForeground(handler, recipeIndex);
        }
    }
}
