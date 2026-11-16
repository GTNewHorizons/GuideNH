package com.hfstudio.guidenh.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.client.FluidStackExtractor;

import codechicken.nei.recipe.StackInfo;

public class NeiFluidStackExtractor implements FluidStackExtractor {

    @Override
    public @Nullable FluidStack getFluid(ItemStack stack) {
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? null : fluid.copy();
    }
}
