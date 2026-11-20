package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

public class NeiRecipeTooltipLines {

    protected NeiRecipeTooltipLines() {}

    public static void append(Object handler, int recipeIndex, @Nullable ItemStack stack,
        @Nullable List<String> positionedTooltip, List<String> lines) {
        GuideNhIntegrationRegistry.global()
            .appendRecipeItemTooltip(handler, stack, lines, recipeIndex);
        if (positionedTooltip != null) {
            for (String line : positionedTooltip) {
                if (line != null && !line.isEmpty()) lines.add(line);
            }
        }
    }
}
