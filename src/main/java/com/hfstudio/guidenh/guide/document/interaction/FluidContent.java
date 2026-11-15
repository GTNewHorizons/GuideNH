package com.hfstudio.guidenh.guide.document.interaction;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public interface FluidContent {

    @Nullable
    FluidStackSpec getFluidSpec();

    default @Nullable FluidStackSpec getFluidSpecAt(int x, int y) {
        return getFluidSpec();
    }
}
