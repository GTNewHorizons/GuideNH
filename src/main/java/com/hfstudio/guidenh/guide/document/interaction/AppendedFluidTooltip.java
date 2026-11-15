package com.hfstudio.guidenh.guide.document.interaction;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;

public class AppendedFluidTooltip extends FluidTooltip {

    private final String extra;

    public AppendedFluidTooltip(FluidStackSpec fluid, String extra) {
        super(fluid, FluidStacks.resolve(fluid));
        this.extra = extra == null ? "" : extra;
    }

    @Override
    public String getText() {
        return extra.isEmpty() ? super.getText() : super.getText() + "\n" + extra;
    }
}
