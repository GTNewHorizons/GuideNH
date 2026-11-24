package com.hfstudio.guidenh.mixins.late.compat.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.Button;
import codechicken.nei.RecipeSearchField;
import codechicken.nei.RestartableTask;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.RecipePageManager;
import codechicken.nei.scroll.ScrollContainer;

@Mixin(value = GuiRecipe.class, remap = false)
public interface AccessorCaptureNeiRecipe {

    @Accessor("searchField")
    static RecipeSearchField guidenh$getCaptureSearch() {
        throw new UnsupportedOperationException();
    }

    @Accessor("toggleSearch")
    static Button guidenh$getCaptureSearchToggle() {
        throw new UnsupportedOperationException();
    }

    @Accessor("updateFilter")
    static RestartableTask guidenh$getCaptureFilterTask() {
        throw new UnsupportedOperationException();
    }

    @Accessor("handlerPages")
    RecipePageManager guidenh$getCapturePages();

    @Accessor("container")
    ScrollContainer guidenh$getCaptureContainer();

    @Invoker("setRecipePage")
    void guidenh$selectCaptureHandler(int index);

    @Invoker("refreshContainer")
    void guidenh$refreshCaptureContainer();
}
