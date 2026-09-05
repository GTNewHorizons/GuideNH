package com.hfstudio.guidenh.guide.color;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record ConstantColor(int color) implements ColorValue {

    public static ConstantColor WHITE = ColorUtils.constant(ColorUtils.WHITE);

    public static ConstantColor BLACK = new ConstantColor(ColorUtils.BLACK.getColor());

    public static ConstantColor TRANSPARENT = ColorUtils.constant(ColorUtils.TRANSPARENT);

    @Override
    public int resolve() {
        return color;
    }
}
