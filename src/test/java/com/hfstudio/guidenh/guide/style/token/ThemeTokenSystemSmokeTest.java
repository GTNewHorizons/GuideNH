package com.hfstudio.guidenh.guide.style.token;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ThemeTokenSystemSmokeTest {

    static final TokenKey<ColorValue> BREAK_COLOR = TokenKey
        .define("--thematic-break-color", TokenType.COLOR, new ColorValue(0xFF373737));
    static final TokenKey<DimensionValue> BREAK_HEIGHT = TokenKey
        .define("--thematic-break-height", TokenType.DIMENSION, new DimensionValue(6, DimensionValue.DimUnit.PX));
    static final TokenKey<FloatValue> ZOOM_STEP = TokenKey.define("--zoom-step", TokenType.FLOAT, new FloatValue(1.1f));
    static final TokenKey<IntValue> TAB_COUNT = TokenKey.define("--tab-count", TokenType.INT, new IntValue(3));

    @Test
    void testIdAssignment() {
        assertTrue(BREAK_COLOR.id() >= 0);
        assertTrue(BREAK_HEIGHT.id() > BREAK_COLOR.id(), "ids should be sequential");
        assertEquals("--thematic-break-color", BREAK_COLOR.name());
    }

    @Test
    void testDefaultsOnBootstrap() {
        GuideThemeManager.instance();
        Theme theme = GuideThemeManager.instance()
            .active();

        assertEquals(
            0xFF373737,
            theme.color(BREAK_COLOR)
                .argb());
        assertEquals(
            6f,
            theme.dim(BREAK_HEIGHT)
                .value());
        assertEquals(
            1.1f,
            theme.flt(ZOOM_STEP)
                .value(),
            0.001f);
        assertEquals(
            3,
            theme.int_(TAB_COUNT)
                .value());
    }

    @Test
    void testHotReloadFromCfgString() {
        // Simulate a .cfg file with overrides
        java.util.Map<String, String> overrides = new java.util.LinkedHashMap<>();
        overrides.put("--thematic-break-color", "0xFFFF0000");
        overrides.put("--thematic-break-height", "12px");
        overrides.put("--zoom-step", "2.5");

        java.util.List<TokenKey<?>> keys = ThemeRegistry.snapshot();
        Theme custom = Theme.build("test", keys, overrides);

        // Overridden values
        assertEquals(
            0xFFFF0000,
            custom.color(BREAK_COLOR)
                .argb());
        assertEquals(
            12f,
            custom.dim(BREAK_HEIGHT)
                .value());
        assertEquals(
            2.5f,
            custom.flt(ZOOM_STEP)
                .value(),
            0.001f);

        // Not overridden → default
        assertEquals(
            3,
            custom.int_(TAB_COUNT)
                .value());
    }

    @Test
    void testParseErrorThrows() {
        // Invalid values should throw: parse functions reject garbage
        assertThrows(Exception.class, () -> ColorValue.parse("garbage_value"));
        assertThrows(Exception.class, () -> DimensionValue.parse("not_a_number"));
    }

    @Test
    void testNameLookup() {
        // Build from the registry snapshot directly: the GuideThemeManager
        // singleton snapshots the registry at first-init time, so depending on
        // suite order it may have been initialized before this class's static
        // TokenKeys were registered (flaky in full-suite runs).
        Theme theme = Theme.build("test", ThemeRegistry.snapshot(), java.util.Collections.emptyMap());

        ColorValue c = theme.get("--thematic-break-color", ColorValue.class);
        assertNotNull(c);
        assertEquals(0xFF373737, c.argb());
    }

    @Test
    void testColorValueParsing() {
        assertEquals(
            0xFFFFFFFF,
            ColorValue.parse("0xFFFFFFFF")
                .argb());
        assertEquals(
            0xFFFF0000,
            ColorValue.parse("0xFFFF0000")
                .argb());
        assertEquals(
            0xFF000000,
            ColorValue.parse("0xFF000000")
                .argb());
        assertEquals(
            0xFF373737,
            ColorValue.parse("#373737")
                .argb());
        assertEquals(
            0xFF00D5FF,
            ColorValue.parse("0, 213, 255")
                .argb());
        assertThrows(Exception.class, () -> ColorValue.parse("garbage"));
    }

    @Test
    void testDimensionValueParsing() {
        assertEquals(
            6f,
            DimensionValue.parse("6px")
                .value());
        assertEquals(
            DimensionValue.DimUnit.PX,
            DimensionValue.parse("6px")
                .unit());
        assertEquals(
            100f,
            DimensionValue.parse("100%")
                .value());
        assertEquals(
            DimensionValue.DimUnit.PCT,
            DimensionValue.parse("100%")
                .unit());
        assertEquals(
            1.5f,
            DimensionValue.parse("1.5em")
                .value());
        assertThrows(Exception.class, () -> DimensionValue.parse("garbage"));
    }

    @Test
    void testTokenTypeParse() {
        assertEquals(0xFF0000FF, ((ColorValue) TokenType.COLOR.parse("0xFF0000FF")).argb());
        assertEquals(6f, ((DimensionValue) TokenType.DIMENSION.parse("6px")).value());
        assertEquals(1.5f, ((FloatValue) TokenType.FLOAT.parse("1.5")).value());
        assertEquals(42, ((IntValue) TokenType.INT.parse("42")).value());
    }
}
