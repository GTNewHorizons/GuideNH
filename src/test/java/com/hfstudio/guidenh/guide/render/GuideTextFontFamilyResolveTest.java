package com.hfstudio.guidenh.guide.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Unit tests for the base/code font-family mapping tables: the single point
 * where config values become wire literals ("system" → null, "mono" →
 * "monospace", empty/null → default, anything else passed through). Pure
 * Java, no JNI dependency.
 */
class GuideTextFontFamilyResolveTest {

    @AfterEach
    void restoreReaderFamilyConfig() {
        ModConfig.reader.readerFontFamily = "system";
        ModConfig.reader.readerCodeFontFamily = "mono";
    }

    @Test
    void defaultConfigBaseFamilyResolvesNull() {
        ModConfig.reader.readerFontFamily = "system";
        assertNull(GuideText.resolveBaseFontFamily(), "default \"system\" must write no family (SansSerif generic)");
    }

    @Test
    void systemKeywordResolvesNullExactMatchOnly() {
        ModConfig.reader.readerFontFamily = "system";
        assertNull(GuideText.resolveBaseFontFamily());
        ModConfig.reader.readerFontFamily = " SYSTEM ";
        assertEquals(
            " SYSTEM ",
            GuideText.resolveBaseFontFamily(),
            "whitespace-padded string is a named family, not the keyword; passthrough");
    }

    @Test
    void defaultConfigCodeFamilyResolvesMonospace() {
        ModConfig.reader.readerCodeFontFamily = "mono";
        assertEquals(
            GuideText.MONO_FONT_FAMILY,
            GuideText.resolveCodeFontFamily(),
            "default \"mono\" must map to the Rust-recognized \"monospace\" wire literal");
    }

    @Test
    void monoKeywordResolvesMonospaceWire() {
        ModConfig.reader.readerCodeFontFamily = "mono";
        assertEquals("monospace", GuideText.resolveCodeFontFamily());
    }

    @Test
    void nullAndBlankResolveToRespectiveDefaults() {
        // Base family default = "system" → null (no wire family).
        ModConfig.reader.readerFontFamily = null;
        assertNull(GuideText.resolveBaseFontFamily());
        ModConfig.reader.readerFontFamily = "";
        assertNull(GuideText.resolveBaseFontFamily());
        ModConfig.reader.readerFontFamily = "   ";
        assertNull(GuideText.resolveBaseFontFamily());
        // Code family default = "mono" → "monospace".
        ModConfig.reader.readerCodeFontFamily = null;
        assertEquals("monospace", GuideText.resolveCodeFontFamily());
        ModConfig.reader.readerCodeFontFamily = "";
        assertEquals("monospace", GuideText.resolveCodeFontFamily());
        ModConfig.reader.readerCodeFontFamily = "  ";
        assertEquals("monospace", GuideText.resolveCodeFontFamily());
    }

    @Test
    void namedFamiliesPassThroughUnchanged() {
        ModConfig.reader.readerFontFamily = "DejaVu Sans";
        assertEquals("DejaVu Sans", GuideText.resolveBaseFontFamily(), "named base family passes through as-is");
        ModConfig.reader.readerFontFamily = "Microsoft YaHei";
        assertEquals("Microsoft YaHei", GuideText.resolveBaseFontFamily());
        ModConfig.reader.readerCodeFontFamily = "Consolas";
        assertEquals("Consolas", GuideText.resolveCodeFontFamily(), "named code family passes through as-is");
        // Case-sensitive passthrough: only exact lowercase keywords map.
        ModConfig.reader.readerFontFamily = "System";
        assertEquals("System", GuideText.resolveBaseFontFamily(), "\"System\" is a named family, not the keyword");
        ModConfig.reader.readerCodeFontFamily = "Mono";
        assertEquals("Mono", GuideText.resolveCodeFontFamily(), "\"Mono\" is a named family, not the keyword");
    }

    @Test
    void fontFamilyOfFollowsResolversForNullFontStyles() {
        // Non-code, null-font style → resolveBaseFontFamily (null under default).
        ModConfig.reader.readerFontFamily = "system";
        assertNull(GuideText.fontFamilyOf(bodyStyle(null)), "base run with null font follows the base resolver");
        ModConfig.reader.readerFontFamily = "Microsoft YaHei";
        assertEquals(
            "Microsoft YaHei",
            GuideText.fontFamilyOf(bodyStyle(null)),
            "base run wire family = resolved base family when the style carries no font");
        // Null style → null.
        assertNull(GuideText.fontFamilyOf(null));
    }

    @Test
    void fontFamilyOfPrefersExplicitStyleFont() {
        ModConfig.reader.readerFontFamily = "Microsoft YaHei";
        assertEquals(
            "Times New Roman",
            GuideText.fontFamilyOf(bodyStyle("Times New Roman")),
            "a style-level named font stays authoritative over the config");
    }

    @Test
    void fontFamilyOfInlineCodeUsesCodeResolver() {
        ModConfig.reader.readerCodeFontFamily = "mono";
        assertEquals("monospace", GuideText.fontFamilyOf(codeStyle(null)));
        ModConfig.reader.readerCodeFontFamily = "Consolas";
        assertEquals(
            "Consolas",
            GuideText.fontFamilyOf(codeStyle(null)),
            "inline code wire family = resolved code family");
    }

    private static ResolvedTextStyle bodyStyle(String font) {
        return new ResolvedTextStyle(
            1f,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            font,
            null,
            null,
            null,
            false,
            null,
            false,
            0f);
    }

    private static ResolvedTextStyle codeStyle(String font) {
        return new ResolvedTextStyle(
            1f,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            font,
            null,
            null,
            null,
            false,
            null,
            true,
            0f);
    }
}
