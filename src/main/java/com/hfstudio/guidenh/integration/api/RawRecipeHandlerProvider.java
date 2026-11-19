package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface RawRecipeHandlerProvider {

    List<Object> queryRawCraftingHandlers(ItemStack target);

    List<Object> queryRawUsageHandlers(ItemStack target);

    default List<Object> queryRawFluidCraftingHandlers(FluidStack target) {
        return List.of();
    }

    default List<Object> queryRawFluidUsageHandlers(FluidStack target) {
        return List.of();
    }
}
