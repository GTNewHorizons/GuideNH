package com.hfstudio.guidenh.guide.style.token;

public record IntValue(int value) implements ResolvedValue {

    public static final IntValue ZERO = new IntValue(0);

    public static IntValue parse(String s) {
        if (s == null || s.isBlank()) return ZERO;
        try {
            return new IntValue(Integer.parseInt(s.strip()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid int value: " + s);
        }
    }
}
