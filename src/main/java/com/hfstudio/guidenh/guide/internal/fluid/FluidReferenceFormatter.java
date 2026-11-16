package com.hfstudio.guidenh.guide.internal.fluid;

import net.minecraftforge.fluids.FluidStack;

public class FluidReferenceFormatter {

    protected FluidReferenceFormatter() {}

    public static String formatTag(String tag, FluidStack stack) {
        FluidStackSpec spec = FluidStackSpec.fromStack(stack, stack.amount > 0);
        var source = new StringBuilder("<").append(tag)
            .append(" id=\"")
            .append(escapeAttribute(spec.id()))
            .append('"');
        if (spec.amount() != null) source.append(" amount={")
            .append(spec.amount())
            .append('}');
        if (spec.nbt() != null) source.append(" nbt=\"")
            .append(escapeAttribute(spec.nbt()))
            .append('"');
        return source.append(" />")
            .toString();
    }

    public static String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
