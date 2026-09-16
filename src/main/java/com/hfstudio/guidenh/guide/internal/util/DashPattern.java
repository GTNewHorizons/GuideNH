package com.hfstudio.guidenh.guide.internal.util;

public record DashPattern(float width, float onLength, float offLength, int color, float animationCycleMs) {

    float length() {
        return onLength + offLength;
    }
}
