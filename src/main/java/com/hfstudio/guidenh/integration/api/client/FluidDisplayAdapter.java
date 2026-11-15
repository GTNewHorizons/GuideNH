package com.hfstudio.guidenh.integration.api.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

public interface FluidDisplayAdapter {

    @Nullable
    FluidStack getDisplayedFluid(ItemStack stack);
}
