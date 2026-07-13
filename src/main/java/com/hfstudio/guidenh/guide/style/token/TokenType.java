package com.hfstudio.guidenh.guide.style.token;

/** Token value type tag. Each type knows how to parse its string form. */
public enum TokenType {
    COLOR {
        @Override public ColorValue parse(String s)   { return ColorValue.parse(s); }
    },
    DIMENSION {
        @Override public DimensionValue parse(String s) { return DimensionValue.parse(s); }
    },
    FLOAT {
        @Override public FloatValue parse(String s)   { return FloatValue.parse(s); }
    },
    INT {
        @Override public IntValue parse(String s)     { return IntValue.parse(s); }
    };

    public abstract ResolvedValue parse(String s);
}
