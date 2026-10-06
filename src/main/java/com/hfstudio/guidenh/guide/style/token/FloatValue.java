package com.hfstudio.guidenh.guide.style.token;

public record FloatValue(float value) implements ResolvedValue {

    public static final FloatValue ZERO = new FloatValue(0f);

    public static FloatValue parse(String s) {
        if (s == null || s.isBlank()) return ZERO;
        try {
            return new FloatValue(Float.parseFloat(s.strip()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid float value: " + s);
        }
    }
}
