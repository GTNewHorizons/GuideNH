package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

public class FluidRecipeRenderer {

    private static final Map<RecipeSlot, FluidStack> STACKS = new WeakHashMap<>();

    protected FluidRecipeRenderer() {}

    private static @Nullable FluidStack stack(RecipeSlot slot) {
        var spec = slot.currentFluid();
        if (spec == null) return null;
        return STACKS.computeIfAbsent(slot, ignored -> FluidStacks.resolve(spec));
    }

    public static boolean draw(RecipeSlot slot, int x, int y, int size) {
        var fluid = stack(slot);
        if (fluid == null) return false;
        FluidIconRenderer.draw(
            fluid,
            x,
            y,
            size,
            slot.currentFluid()
                .amount() != null);
        return true;
    }

    public static @Nullable FluidTooltip tooltip(RecipeSlot slot) {
        var fluid = stack(slot);
        return fluid == null ? null : new FluidTooltip(slot.currentFluid(), fluid);
    }
}
