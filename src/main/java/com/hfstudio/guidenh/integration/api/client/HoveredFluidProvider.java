package com.hfstudio.guidenh.integration.api.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

public interface HoveredFluidProvider {

    @Nullable
    FluidStack getHoveredFluid(GuiScreen screen, int mouseX, int mouseY);
}
