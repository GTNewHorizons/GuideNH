package com.hfstudio.guidenh.integration.api.client;

import java.util.List;

import net.minecraftforge.fluids.FluidStack;

public interface FluidTooltipProvider {

    void appendTooltip(FluidStack stack, List<String> tooltip);
}
