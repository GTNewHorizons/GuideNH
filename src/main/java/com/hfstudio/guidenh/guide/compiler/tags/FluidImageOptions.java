package com.hfstudio.guidenh.guide.compiler.tags;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.LytFluidImage;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public record FluidImageOptions(FluidStackSpec fluid, float scale, boolean showTooltip, boolean showIcon,
    @Nullable String labelPosition, @Nullable String format, @Nullable Integer yOffset,
    @Nullable Integer labelYOffset) {

    public FluidImageOptions {
        if (fluid == null) throw new IllegalArgumentException("Fluid description is required");
        if (!Float.isFinite(scale) || scale <= 0)
            throw new IllegalArgumentException("scale must be finite and positive");
        scale = Math.max(0.125f, scale);
    }

    public static FluidImageOptions fromImage(LytFluidImage image) {
        return new FluidImageOptions(
            image.getFluidSpec(),
            image.getScale(),
            image.isShowTooltip(),
            image.isShowingIcon(),
            image.getLabelPosition(),
            image.getLabelFormat(),
            image.getInlineYOffsetOverride(),
            image.getLabelYOffsetOverride());
    }

    public static FluidImageOptions read(MdxJsxElementFields element) {
        return new FluidImageOptions(
            FluidTagAttributes.readFluid(element),
            FluidTagAttributes.scale(element),
            FluidTagAttributes.showTooltip(element),
            MdxAttrs.getBoolean(element, "showIcon", true),
            ItemImageCompiler.resolveLabelPosition(element.getAttributeString("label", null)),
            element.getAttributeString("format", null),
            FluidTagAttributes.integer(element, "yOffset"),
            FluidTagAttributes.integer(element, "labelYOffset"));
    }

    public LytFluidImage createImage() {
        LytFluidImage image = new LytFluidImage(fluid);
        image.setInline(true);
        image.setScale(scale);
        image.setShowTooltip(showTooltip);
        image.setShowIcon(showIcon);
        image.setLabelPosition(labelPosition);
        image.setLabelFormat(format);
        image.setInlineYOffsetOverride(yOffset);
        image.setLabelYOffsetOverride(labelYOffset);
        return image;
    }
}
