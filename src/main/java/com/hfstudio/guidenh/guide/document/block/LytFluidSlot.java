package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytFluidSlot extends LytFluidImage {

    public LytFluidSlot(FluidStackSpec spec) {
        super(spec);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(x, y, LytSlot.OUTER_SIZE, LytSlot.OUTER_SIZE);
    }

    @Override
    public void render(RenderContext context) {
        LytSlot.drawSlotBackground(context, bounds);
        renderIcon(context, bounds.x() + LytSlot.PADDING, bounds.y() + LytSlot.PADDING);
    }
}
