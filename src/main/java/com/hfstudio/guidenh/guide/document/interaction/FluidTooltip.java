package com.hfstudio.guidenh.guide.document.interaction;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidTooltips;

public class FluidTooltip extends TextTooltip implements FluidContent {

    private final FluidStackSpec spec;
    private final FluidStack stack;

    public FluidTooltip(FluidStackSpec spec, FluidStack stack) {
        super("");
        this.spec = spec;
        this.stack = stack.copy();
    }

    @Override
    public FluidStackSpec getFluidSpec() {
        return spec;
    }

    public FluidStack getStack() {
        return stack.copy();
    }

    @Override
    public String getText() {
        return String
            .join("\n", FluidTooltips.lines(spec, stack, Minecraft.getMinecraft().gameSettings.advancedItemTooltips));
    }
}
