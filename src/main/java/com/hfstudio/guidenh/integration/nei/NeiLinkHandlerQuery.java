package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.nei.NeiLinkTarget;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.RecipeHandlerQuery;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NeiLinkHandlerQuery {

    private NeiLinkHandlerQuery() {}

    public static List<ICraftingHandler> craftingHandlers(NeiLinkTarget target, @Nullable ItemStack stack) {
        RecipeHandlerQuery<ICraftingHandler> query = new RecipeHandlerQuery<>(
            handler -> stack == null ? allRecipes(handler, target.handlerId())
                : handler.getRecipeHandler("item", stack.copy()),
            select(GuiCraftingRecipe.craftinghandlers, target.handlerId()),
            select(GuiCraftingRecipe.serialCraftingHandlers, target.handlerId()),
            "Error while opening GuideNH NEI recipes",
            target.toString());
        return select(query.runWithProfiling("guidenh.nei.recipes"), target.handlerId());
    }

    public static List<IUsageHandler> usageHandlers(NeiLinkTarget target, ItemStack stack) {
        RecipeHandlerQuery<IUsageHandler> query = new RecipeHandlerQuery<>(
            handler -> handler.getUsageAndCatalystHandler("item", stack.copy()),
            select(GuiUsageRecipe.usagehandlers, target.handlerId()),
            select(GuiUsageRecipe.serialUsageHandlers, target.handlerId()),
            "Error while opening GuideNH NEI usages",
            target.toString());
        return select(query.runWithProfiling("guidenh.nei.usage"), target.handlerId());
    }

    private static ICraftingHandler allRecipes(ICraftingHandler handler, @Nullable String handlerId) {
        ICraftingHandler result = handler instanceof TemplateRecipeHandler template ? template.getAllRecipeHandler()
            : handler.getRecipeHandler("all");
        if (result != null && result.numRecipes() > 0) return result;
        String overlay = handler.getOverlayIdentifier();
        if (overlay != null && !overlay.isBlank()) {
            result = handler.getRecipeHandler(overlay);
            if (result != null && result.numRecipes() > 0) return result;
        }
        return handlerId == null ? result : handler.getRecipeHandler(handlerId);
    }

    private static <T extends IRecipeHandler> List<T> select(List<T> handlers, @Nullable String handlerId) {
        if (handlerId == null) return new ArrayList<>(handlers);
        List<T> selected = new ArrayList<>();
        for (T handler : handlers) {
            if (handlerId.equalsIgnoreCase(handler.getOverlayIdentifier())
                || handlerId.equalsIgnoreCase(handler.getHandlerId())) {
                selected.add(handler);
            }
        }
        return selected;
    }
}
