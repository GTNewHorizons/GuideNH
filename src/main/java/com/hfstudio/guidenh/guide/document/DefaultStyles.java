package com.hfstudio.guidenh.guide.document;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class DefaultStyles {

    private DefaultStyles() {}

    public static final String UNIFORM_FONT = null;

    /**
     * The base style everything else is based on.
     */
    public static final ResolvedTextStyle BASE_STYLE = new ResolvedTextStyle(
        1,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        UNIFORM_FONT,
        SymbolicColor.BODY_TEXT,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false,
        0.0f);

    public static final TextStyle BODY_TEXT = TextStyle.builder()
        .font(UNIFORM_FONT)
        .color(SymbolicColor.BODY_TEXT)
        .build();

    public static final TextStyle ERROR_TEXT = TextStyle.builder()
        .color(SymbolicColor.ERROR_TEXT)
        .build();

    public static final TextStyle CRAFTING_RECIPE_TYPE = TextStyle.builder()
        .font(UNIFORM_FONT)
        .color(SymbolicColor.CRAFTING_RECIPE_TYPE)
        .build();

    /**
     * Heading size ladder — strictly monotonic decreasing (H1 > H2 > ... > H6)
     * so section depth reads from glyph size alone. All headings are bold and
     * white to stay clearly distinct from the regular gray body text
     * ({@link SymbolicColor#BODY_TEXT} #d2d2d2), compensating for the low
     * contrast between title white and body gray.
     */
    public static final TextStyle HEADING1 = TextStyle.builder()
        .fontScale(1.5f)
        .bold(true)
        .font(null)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING2 = TextStyle.builder()
        .fontScale(1.3f)
        .bold(true)
        .font(null)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING3 = TextStyle.builder()
        .fontScale(1.18f)
        .bold(true)
        .font(null)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING4 = TextStyle.builder()
        .fontScale(1.08f)
        .bold(true)
        .font(UNIFORM_FONT)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING5 = TextStyle.builder()
        .fontScale(1f)
        .bold(true)
        .font(UNIFORM_FONT)
        .color(ConstantColor.WHITE)
        .build();
    public static final TextStyle HEADING6 = TextStyle.builder()
        .fontScale(0.95f)
        .bold(true)
        .font(UNIFORM_FONT)
        .color(ConstantColor.WHITE)
        .build();

    public static final TextStyle SEARCH_RESULT_HIGHLIGHT = TextStyle.builder()
        .bold(true)
        .underlined(true)
        .build();
}
