package com.hfstudio.guidenh.guide.style.token;

/** ARGB color token value. Semantic constants are pre-defined statics. */
public record ColorValue(int argb) implements ResolvedValue {

    public static final ColorValue WHITE = new ColorValue(0xFFFFFFFF);
    public static final ColorValue BLACK = new ColorValue(0xFF000000);
    public static final ColorValue TRANSPARENT = new ColorValue(0x00000000);
    public static final ColorValue RED = new ColorValue(0xFFFF0000);
    public static final ColorValue GREEN = new ColorValue(0xFF00FF00);
    public static final ColorValue BLUE = new ColorValue(0xFF0000FF);

    /** Parse from hex string: "0xFF373737", "#373737", "255,55,55" */
    public static ColorValue parse(String s) {
        if (s == null || s.isBlank()) return TRANSPARENT;
        s = s.strip();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return new ColorValue((int) Long.parseLong(s.substring(2), 16));
            }
            if (s.startsWith("#")) {
                String hex = s.substring(1);
                if (hex.length() == 6) hex = "FF" + hex;
                return new ColorValue((int) Long.parseLong(hex, 16));
            }
            String[] parts = s.split(",");
            if (parts.length >= 3) {
                int r = Integer.parseInt(parts[0].strip());
                int g = Integer.parseInt(parts[1].strip());
                int b = Integer.parseInt(parts[2].strip());
                int a = parts.length >= 4 ? Integer.parseInt(parts[3].strip()) : 255;
                return new ColorValue((a << 24) | (r << 16) | (g << 8) | b);
            }
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Invalid color value: " + s);
    }

    public int red() {
        return (argb >>> 16) & 0xFF;
    }

    public int green() {
        return (argb >>> 8) & 0xFF;
    }

    public int blue() {
        return argb & 0xFF;
    }

    public int alpha() {
        return (argb >>> 24) & 0xFF;
    }
}
