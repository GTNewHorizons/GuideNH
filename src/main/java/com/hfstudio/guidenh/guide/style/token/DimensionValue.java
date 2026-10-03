package com.hfstudio.guidenh.guide.style.token;

public record DimensionValue(float value, DimUnit unit) implements ResolvedValue {

    public static final DimensionValue ZERO = new DimensionValue(0, DimUnit.PX);

    public static DimensionValue px(float px) {
        return new DimensionValue(px, DimUnit.PX);
    }

    public static DimensionValue pct(float pct) {
        return new DimensionValue(pct, DimUnit.PCT);
    }

    /** Parse: "6px", "100%", "1.5em", "0" (defaults to px) */
    public static DimensionValue parse(String s) {
        if (s == null || s.isBlank()) return ZERO;
        s = s.strip()
            .toLowerCase();
        try {
            if (s.endsWith("px")) return px(
                Float.parseFloat(
                    s.substring(0, s.length() - 2)
                        .strip()));
            if (s.endsWith("%")) return pct(
                Float.parseFloat(
                    s.substring(0, s.length() - 1)
                        .strip()));
            if (s.endsWith("em")) return new DimensionValue(
                Float.parseFloat(
                    s.substring(0, s.length() - 2)
                        .strip()),
                DimUnit.EM);
            return px(Float.parseFloat(s));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid dimension value: " + s);
        }
    }

    public int pxInt() {
        return Math.round(value);
    }

    public enum DimUnit {
        PX,
        PCT,
        EM
    }
}
