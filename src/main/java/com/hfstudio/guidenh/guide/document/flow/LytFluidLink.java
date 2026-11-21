package com.hfstudio.guidenh.guide.document.flow;

import com.hfstudio.guidenh.guide.document.interaction.FluidContent;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public class LytFluidLink extends LytFlowLink implements FluidContent {

    private final FluidStackSpec spec;

    public LytFluidLink(FluidStackSpec spec) {
        this.spec = spec;
    }

    @Override
    public FluidStackSpec getFluidSpec() {
        return spec;
    }
}
