package com.hfstudio.guidenh.guide.style;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;

public record BorderStyle(ColorValue color, int width) {

    public static BorderStyle NONE = new BorderStyle(ConstantColor.TRANSPARENT, 0);
}
