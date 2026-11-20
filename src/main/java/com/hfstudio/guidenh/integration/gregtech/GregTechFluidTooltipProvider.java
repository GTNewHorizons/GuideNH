package com.hfstudio.guidenh.integration.gregtech;

import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.integration.api.client.FluidTooltipProvider;

import gregtech.common.items.ItemFluidDisplay;

public class GregTechFluidTooltipProvider implements FluidTooltipProvider {

    @Override
    public void appendTooltip(FluidStack stack, List<String> tooltip) {
        ItemFluidDisplay.addTooltipForFluid(stack.getFluid(), tooltip);
    }
}
