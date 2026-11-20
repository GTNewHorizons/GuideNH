package com.hfstudio.guidenh.integration.api;

public interface RecipeHandlerRenderProvider {

    boolean canRenderRecipeHandler(Object handler);

    void renderRecipeHandler(Object handler, int recipeIndex, boolean skipForeground);

    default void renderRecipeHandlerBackground(Object handler, int recipeIndex, boolean skipForeground) {
        renderRecipeHandler(handler, recipeIndex, skipForeground);
    }

    default void renderRecipeHandlerForeground(Object handler, int recipeIndex, boolean skipForeground) {}
}
