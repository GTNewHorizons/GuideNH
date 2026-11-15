package com.hfstudio.guidenh.guide.document.interaction;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;

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
        String text = FluidStacks.displayName(stack);
        if (spec.amount() != null) text += "\n" + NumberFormatUtil.formatNumber(stack.amount) + " L";
        if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) text += "\n\u00a78" + spec.id();
        return text;
    }
}
