package com.hfstudio.guidenh.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.client.FluidDisplayAdapter;

import codechicken.nei.api.IStackStringifyHandler;
import codechicken.nei.recipe.StackInfo;

public class NeiFluidDisplayAdapter implements FluidDisplayAdapter {

    @Override
    public @Nullable FluidStack getDisplayedFluid(ItemStack stack) {
        for (int i = StackInfo.stackStringifyHandlers.size() - 1; i >= 0; i--) {
            IStackStringifyHandler handler = StackInfo.stackStringifyHandlers.get(i);
            if (handler.isFluidDisplayItem(stack)) {
                FluidStack fluid = StackInfo.getFluid(stack);
                return fluid == null ? null : fluid.copy();
            }
        }
        return null;
    }
}
