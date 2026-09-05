package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import java.util.EnumMap;
import java.util.Map;

public class CodeHighlightTheme {

    public static final CodeHighlightTheme GITHUB_DARK_DEFAULT = githubDarkDefault();

    private final int backgroundArgb;
    private final int toolbarBackgroundArgb;
    private final int borderArgb;
    private final int scrollbarTrackArgb;
    private final int scrollbarThumbArgb;
    private final int scrollbarThumbActiveArgb;
    private final int toolbarTextArgb;
    private final Map<CodeTokenType, Integer> tokenColors;

    public CodeHighlightTheme(int backgroundArgb, int toolbarBackgroundArgb, int borderArgb, int scrollbarTrackArgb,
        int scrollbarThumbArgb, int scrollbarThumbActiveArgb, int toolbarTextArgb,
        Map<CodeTokenType, Integer> tokenColors) {
        this.backgroundArgb = backgroundArgb;
        this.toolbarBackgroundArgb = toolbarBackgroundArgb;
        this.borderArgb = borderArgb;
        this.scrollbarTrackArgb = scrollbarTrackArgb;
        this.scrollbarThumbArgb = scrollbarThumbArgb;
        this.scrollbarThumbActiveArgb = scrollbarThumbActiveArgb;
        this.toolbarTextArgb = toolbarTextArgb;
        this.tokenColors = Map.copyOf(tokenColors);
    }

    public int backgroundArgb() {
        return backgroundArgb;
    }

    public int toolbarBackgroundArgb() {
        return toolbarBackgroundArgb;
    }

    public int borderArgb() {
        return borderArgb;
    }

    public int scrollbarTrackArgb() {
        return scrollbarTrackArgb;
    }

    public int scrollbarThumbArgb() {
        return scrollbarThumbArgb;
    }

    public int scrollbarThumbActiveArgb() {
        return scrollbarThumbActiveArgb;
    }

    public int toolbarTextArgb() {
        return toolbarTextArgb;
    }

    public int colorOf(CodeTokenType type) {
        return tokenColors.getOrDefault(type, tokenColors.get(CodeTokenType.PLAIN));
    }

    private static CodeHighlightTheme githubDarkDefault() {
        Map<CodeTokenType, Integer> colors = new EnumMap<>(CodeTokenType.class);
        colors.put(CodeTokenType.PLAIN, ColorUtils.ARGB_FFE6EDF3.getColor());
        colors.put(CodeTokenType.KEYWORD, ColorUtils.ARGB_FFFF7B72.getColor());
        colors.put(CodeTokenType.STRING, ColorUtils.ARGB_FFA5D6FF.getColor());
        colors.put(CodeTokenType.NUMBER, ColorUtils.ARGB_FF79C0FF.getColor());
        colors.put(CodeTokenType.COMMENT, ColorUtils.ARGB_FF8B949E.getColor());
        colors.put(CodeTokenType.OPERATOR, ColorUtils.ARGB_FFFF7B72.getColor());
        colors.put(CodeTokenType.PUNCTUATION, ColorUtils.ARGB_FFE6EDF3.getColor());
        colors.put(CodeTokenType.TYPE, ColorUtils.ARGB_FF7EE787.getColor());
        colors.put(CodeTokenType.FUNCTION, ColorUtils.ARGB_FFD2A8FF.getColor());
        colors.put(CodeTokenType.ANNOTATION, ColorUtils.ARGB_FFFFA657.getColor());
        colors.put(CodeTokenType.PROPERTY, ColorUtils.ARGB_FF79C0FF.getColor());
        return new CodeHighlightTheme(
            ColorUtils.ARGB_FF0D1117.getColor(),
            ColorUtils.ARGB_FF161B22.getColor(),
            ColorUtils.ARGB_FF30363D.getColor(),
            ColorUtils.ARGB_4D6E7681.getColor(),
            ColorUtils.ARGB_80768496.getColor(),
            ColorUtils.ARGB_CC768496.getColor(),
            ColorUtils.ARGB_FF8B949E.getColor(),
            colors);
    }
}
