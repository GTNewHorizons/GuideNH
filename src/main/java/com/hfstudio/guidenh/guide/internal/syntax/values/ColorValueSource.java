package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests the symbolic colour names accepted by colour attributes. */
public class ColorValueSource implements SyntaxValueSource {

    private static final List<SyntaxSuggestion> SYMBOLIC_NAMES = List.of(
        SyntaxSuggestion.of("LINK"),
        SyntaxSuggestion.of("BODY_TEXT"),
        SyntaxSuggestion.of("ERROR_TEXT"),
        SyntaxSuggestion.of("CRAFTING_RECIPE_TYPE"),
        SyntaxSuggestion.of("THEMATIC_BREAK"),
        SyntaxSuggestion.of("HEADER1_SEPARATOR"),
        SyntaxSuggestion.of("HEADER2_SEPARATOR"),
        SyntaxSuggestion.of("NAVBAR_BG_TOP"),
        SyntaxSuggestion.of("NAVBAR_BG_BOTTOM"),
        SyntaxSuggestion.of("NAVBAR_ROW_HOVER"),
        SyntaxSuggestion.of("NAVBAR_EXPAND_ARROW"),
        SyntaxSuggestion.of("TABLE_BORDER"),
        SyntaxSuggestion.of("ICON_BUTTON_NORMAL"),
        SyntaxSuggestion.of("ICON_BUTTON_DISABLED"),
        SyntaxSuggestion.of("ICON_BUTTON_HOVER"),
        SyntaxSuggestion.of("IN_WORLD_BLOCK_HIGHLIGHT"),
        SyntaxSuggestion.of("SCENE_BACKGROUND"),
        SyntaxSuggestion.of("GUIDE_SCREEN_BACKGROUND"),
        SyntaxSuggestion.of("BLOCKQUOTE_BACKGROUND"),
        SyntaxSuggestion.of("BLACK"),
        SyntaxSuggestion.of("DARK_BLUE"),
        SyntaxSuggestion.of("DARK_GREEN"),
        SyntaxSuggestion.of("DARK_AQUA"),
        SyntaxSuggestion.of("DARK_RED"),
        SyntaxSuggestion.of("DARK_PURPLE"),
        SyntaxSuggestion.of("GOLD"),
        SyntaxSuggestion.of("GRAY"),
        SyntaxSuggestion.of("DARK_GRAY"),
        SyntaxSuggestion.of("BLUE"),
        SyntaxSuggestion.of("GREEN"),
        SyntaxSuggestion.of("AQUA"),
        SyntaxSuggestion.of("RED"),
        SyntaxSuggestion.of("LIGHT_PURPLE"),
        SyntaxSuggestion.of("YELLOW"),
        SyntaxSuggestion.of("WHITE"));

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.COLOR);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return SyntaxSuggestion.containing(SYMBOLIC_NAMES, request.partialText(), limit);
    }
}
