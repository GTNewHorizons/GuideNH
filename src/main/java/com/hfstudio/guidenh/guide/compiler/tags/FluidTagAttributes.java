package com.hfstudio.guidenh.guide.compiler.tags;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class FluidTagAttributes {

    protected FluidTagAttributes() {}

    public static FluidStackSpec readFluid(MdxJsxElementFields element) {
        if (element.getAttributeString("ore", null) != null) {
            throw new IllegalArgumentException("Fluid tags do not accept item ore selectors");
        }
        return new FluidStackSpec(
            element.getAttributeString("id", null),
            integer(element, "amount"),
            element.getAttributeString("nbt", null));
    }

    public static float scale(MdxJsxElementFields element) {
        String raw = element.getAttributeString("scale", null);
        float scale = raw == null ? 1f : Float.parseFloat(raw.trim());
        if (!Float.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("scale must be finite and positive");
        }
        return scale;
    }

    public static boolean showTooltip(MdxJsxElementFields element) {
        Boolean visible = MdxAttrs.getOptionalBoolean(element, "showTooltip");
        return visible != null ? visible : !MdxAttrs.getBoolean(element, "noTooltip", false);
    }

    public static @Nullable Integer integer(MdxJsxElementFields element, String name) {
        String raw = element.getAttributeString(name, null);
        if (raw == null) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }
}
