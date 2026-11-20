package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public class NeiFluidTooltip extends FluidTooltip {

    private final @Nullable ItemStack displayStack;
    private final Object handler;
    private final int recipeIndex;
    private final List<String> positionedTooltip;

    public NeiFluidTooltip(FluidStackSpec spec, FluidStack stack, @Nullable ItemStack displayStack, Object handler,
        int recipeIndex, @Nullable List<String> positionedTooltip) {
        super(spec, stack);
        this.displayStack = displayStack;
        this.handler = handler;
        this.recipeIndex = recipeIndex;
        this.positionedTooltip = positionedTooltip == null ? List.of() : List.copyOf(positionedTooltip);
    }

    @Override
    public String getText() {
        List<String> lines = new ArrayList<>(List.of(super.getText().split("\n")));
        NeiRecipeTooltipLines.append(handler, recipeIndex, displayStack, positionedTooltip, lines);
        return String.join("\n", lines);
    }
}
