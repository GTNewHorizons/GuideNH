package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

public class FluidRecipeSlots {

    protected FluidRecipeSlots() {}

    public static Set<String> ids(List<RecipeSlot> slots) {
        var ids = new HashSet<String>();
        for (var slot : slots) {
            if (slot != null) slot.fluids()
                .forEach(fluid -> ids.add(fluid.id()));
        }
        return ids;
    }

    public static boolean matches(Object handler, int recipeIndex, @Nullable String target, boolean usage,
        boolean includeInputs, FluidFilterExpression input, FluidFilterExpression output) {
        var registry = GuideNhIntegrationRegistry.global();
        var ingredients = registry.readRecipeIngredientSlots(handler, recipeIndex);
        var others = registry.readRecipeOtherSlots(handler, recipeIndex);
        var result = registry.readRecipeResultSlot(handler, recipeIndex);
        return matches(ingredients, others, result, target, usage, includeInputs, input, output);
    }

    public static boolean matches(List<RecipeSlot> ingredients, List<RecipeSlot> others, @Nullable RecipeSlot result,
        @Nullable String target, boolean usage, boolean includeInputs, FluidFilterExpression input,
        FluidFilterExpression output) {
        if (target == null && input.isEmpty() && output.isEmpty()) return true;
        Set<String> inputs = ids(ingredients);
        Set<String> auxiliary = ids(others);
        Set<String> outputs = new HashSet<>(auxiliary);
        if (result != null) outputs.addAll(ids(List.of(result)));
        if (!input.matches(inputs) || !output.matches(outputs)) return false;
        if (target == null) return true;
        if (usage) return inputs.contains(target) || auxiliary.contains(target);
        return outputs.contains(target) || includeInputs && inputs.contains(target);
    }
}
