package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.document.interaction.FluidContent;
import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytPageFluidIcon extends LytIconImage implements FluidContent {

    private final GuidePageIcon icon;

    public LytPageFluidIcon(GuidePageIcon icon) {
        if (!icon.isFluidIcon()) throw new IllegalArgumentException("A fluid page icon is required");
        this.icon = icon;
    }

    @Override
    public FluidStackSpec getFluidSpec() {
        return icon.resolveCurrentFluidSpec();
    }

    @Override
    protected String getDisplayName() {
        return FluidStacks.displayName(FluidStacks.resolve(getFluidSpec()));
    }

    @Override
    protected boolean hasContent() {
        return true;
    }

    @Override
    protected void renderIcon(RenderContext context, int x, int y) {
        icon.renderCurrentFluid(context, x, y, Math.round(BASE_SIZE * getScale()));
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!isTooltipVisible()) return Optional.empty();
        FluidStackSpec spec = getFluidSpec();
        return Optional.of(new FluidTooltip(spec, FluidStacks.resolve(spec)));
    }
}
