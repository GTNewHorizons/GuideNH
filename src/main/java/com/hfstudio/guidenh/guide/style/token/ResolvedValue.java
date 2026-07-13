package com.hfstudio.guidenh.guide.style.token;

/** All token value types implement this sealed interface. */
public sealed interface ResolvedValue
    permits ColorValue, DimensionValue, FloatValue, IntValue {
}
