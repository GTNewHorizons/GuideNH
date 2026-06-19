package com.hfstudio.guidenh.guide.latex;

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scilab.forge.jlatexmath.JavaFontRenderingBox;
import org.scilab.forge.jlatexmath.TeXFormula;

public class GuideLatexExternalFonts {

    private static final String DEFAULT_FONT = "Serif";
    private static final Object EXTERNAL_FONT_LOCK = new Object();
    private static final List<UnicodeBlock> TEXT_BLOCKS = List.of(
        UnicodeBlock.BASIC_LATIN,
        UnicodeBlock.LATIN_1_SUPPLEMENT,
        UnicodeBlock.LATIN_EXTENDED_A,
        UnicodeBlock.LATIN_EXTENDED_B,
        UnicodeBlock.GREEK,
        UnicodeBlock.CYRILLIC,
        UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        UnicodeBlock.HIRAGANA,
        UnicodeBlock.KATAKANA,
        UnicodeBlock.HANGUL_SYLLABLES,
        UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);

    protected GuideLatexExternalFonts() {}

    public static <T> T withProfile(GuideLatexFontProfile profile, LatexRenderAction<T> action) throws Exception {
        if (profile == null || !profile.usesExternalFont()) {
            return action.run();
        }
        synchronized (EXTERNAL_FONT_LOCK) {
            Map<UnicodeBlock, Object> map = externalFontMap();
            List<BlockFontState> previous = captureTextBlockState(map);
            try {
                applyExternalFont(profile.externalFontName());
                return action.run();
            } finally {
                restoreTextBlockState(map, previous);
                JavaFontRenderingBox.setFont(DEFAULT_FONT);
            }
        }
    }

    public static void apply(GuideLatexFontProfile profile) {
        String fontName = profile != null ? profile.externalFontName() : "";
        if (fontName == null || fontName.isBlank()) {
            restoreDefault();
            return;
        }
        applyExternalFont(fontName);
    }

    public static void restoreDefault() {
        JavaFontRenderingBox.setFont(DEFAULT_FONT);
        for (UnicodeBlock block : TEXT_BLOCKS) {
            TeXFormula.registerExternalFont(block, null, null);
        }
    }

    private static void applyExternalFont(String fontName) {
        JavaFontRenderingBox.setFont(fontName);
        for (UnicodeBlock block : TEXT_BLOCKS) {
            TeXFormula.registerExternalFont(block, fontName);
        }
    }

    private static List<BlockFontState> captureTextBlockState(Map<UnicodeBlock, Object> map) {
        var states = new ArrayList<BlockFontState>(TEXT_BLOCKS.size());
        for (UnicodeBlock block : TEXT_BLOCKS) {
            states.add(new BlockFontState(block, map.containsKey(block), map.get(block)));
        }
        return states;
    }

    private static void restoreTextBlockState(Map<UnicodeBlock, Object> map, List<BlockFontState> previous) {
        for (BlockFontState state : previous) {
            if (state.existed()) {
                map.put(state.block(), state.value());
            } else {
                map.remove(state.block());
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map<UnicodeBlock, Object> externalFontMap() {
        return (Map) TeXFormula.externalFontMap;
    }

    @FunctionalInterface
    public interface LatexRenderAction<T> {

        T run() throws Exception;
    }

    private record BlockFontState(UnicodeBlock block, boolean existed, Object value) {}
}
