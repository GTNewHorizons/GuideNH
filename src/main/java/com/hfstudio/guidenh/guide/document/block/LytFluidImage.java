package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.guide.document.interaction.FluidContent;
import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class LytFluidImage extends LytIconImage implements FluidContent {

    private final FluidStackSpec spec;
    private final FluidStack stack;
    private final FluidTooltip tooltip;
    private boolean renderFailed;

    public LytFluidImage(FluidStackSpec spec) {
        this.spec = spec;
        this.stack = FluidStacks.resolve(spec);
        this.tooltip = new FluidTooltip(spec, stack);
    }

    @Override
    public FluidStackSpec getFluidSpec() {
        return spec;
    }

    public FluidStack getStack() {
        return stack.copy();
    }

    @Override
    protected String getDisplayName() {
        return FluidStacks.displayName(stack);
    }

    @Override
    protected boolean hasContent() {
        return true;
    }

    @Override
    protected void renderIcon(RenderContext context, int x, int y) {
        if (renderFailed) return;
        try {
            FluidIconRenderer.draw(context, stack, x, y, Math.round(BASE_SIZE * getScale()), spec.amount() != null);
        } catch (RuntimeException exception) {
            renderFailed = true;
            GuideDebugLog.warn("[GuideNH] Fluid icon rendering failed for {}: {}", spec.id(), exception.toString());
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return isTooltipVisible() ? Optional.of(tooltip) : Optional.empty();
    }
}
