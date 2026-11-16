package com.hfstudio.guidenh.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.integration.api.RawRecipeHandlerProvider;

import codechicken.nei.item.ItemFluidDisplay;

public class NeiRawRecipeHandlerProvider implements RawRecipeHandlerProvider {

    @Override
    public List<Object> queryRawCraftingHandlers(ItemStack target) {
        return NeiRecipeLookup.queryRawCraftingHandlers(target);
    }

    @Override
    public List<Object> queryRawUsageHandlers(ItemStack target) {
        return NeiRecipeLookup.queryRawUsageHandlers(target);
    }

    @Override
    public List<Object> queryRawFluidCraftingHandlers(FluidStack target) {
        return NeiRecipeLookup.queryRawCraftingHandlers(ItemFluidDisplay.createStack(target));
    }

    @Override
    public List<Object> queryRawFluidUsageHandlers(FluidStack target) {
        return NeiRecipeLookup.queryRawUsageHandlers(ItemFluidDisplay.createStack(target));
    }
}
