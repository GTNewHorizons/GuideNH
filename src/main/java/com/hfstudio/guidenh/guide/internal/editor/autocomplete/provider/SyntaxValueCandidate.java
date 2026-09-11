package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

/**
 * A value suggested by a {@code SyntaxValueSource}, rendered with its optional icon and subtitle. The
 * value kind is kept so the commit step knows whether the value is quoted or braced.
 */
public class SyntaxValueCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int SUBTITLE_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 2;
    private static final String LABEL_GAP = "  ";

    private final SyntaxSuggestion suggestion;
    private final SyntaxValueKind kind;

    public SyntaxValueCandidate(SyntaxSuggestion suggestion, SyntaxValueKind kind) {
        this.suggestion = suggestion;
        this.kind = kind;
    }

    @Override
    public String displayText() {
        return suggestion.displayText();
    }

    @Override
    public String replacementText() {
        return suggestion.value();
    }

    @Override
    public boolean quotesValue() {
        return kind.isQuoted();
    }

    @Override
    public int renderHeight() {
        return suggestion.icon() != null ? ICON_SIZE + 2 : 14;
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        int width = fontRenderer.getStringWidth(suggestion.displayText());
        String subtitle = suggestion.subtitle();
        if (subtitle != null && !subtitle.isEmpty()) {
            width += fontRenderer.getStringWidth(LABEL_GAP) + fontRenderer.getStringWidth(subtitle);
        }
        return suggestion.icon() != null ? width + ICON_SIZE + ICON_GAP : width;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int textX = x;
        if (suggestion.icon() != null) {
            ItemCandidate.renderIcon(suggestion.icon(), x, y);
            textX += ICON_SIZE + ICON_GAP;
        }
        int labelWidth = fontRenderer.drawString(suggestion.displayText(), textX, y + 4, LABEL_COLOR);
        String subtitle = suggestion.subtitle();
        if (subtitle != null && !subtitle.isEmpty()) {
            fontRenderer.drawString(
                subtitle,
                textX + labelWidth + fontRenderer.getStringWidth(LABEL_GAP),
                y + 4,
                SUBTITLE_COLOR);
        }
    }
}
