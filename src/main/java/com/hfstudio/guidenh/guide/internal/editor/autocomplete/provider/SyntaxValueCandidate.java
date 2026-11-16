package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;

public class SyntaxValueCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int SUBTITLE_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 2;
    private static final String LABEL_GAP = "  ";

    private final SyntaxSuggestion suggestion;
    private final SyntaxValueKind kind;
    private final String label;
    private final String subtitle;

    public SyntaxValueCandidate(SyntaxSuggestion suggestion, SyntaxValueKind kind) {
        this.suggestion = suggestion;
        this.kind = kind;
        this.label = CompletionPreview.summarize(suggestion.displayText());
        this.subtitle = CompletionPreview.summarize(suggestion.subtitle());
    }

    @Override
    public String displayText() {
        return label;
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
    public SyntaxSuggestion syntaxSuggestion() {
        return suggestion;
    }

    @Override
    public int renderHeight() {
        return suggestion.hasIcon() ? ICON_SIZE + 2 : 14;
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        int width = fontRenderer.getStringWidth(label);
        if (!subtitle.isEmpty()) {
            width += fontRenderer.getStringWidth(LABEL_GAP) + fontRenderer.getStringWidth(subtitle);
        }
        return suggestion.hasIcon() ? width + ICON_SIZE + ICON_GAP : width;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int textX = x;
        if (suggestion.icon() != null) {
            ItemCandidate.renderIcon(suggestion.icon(), x, y);
            textX += ICON_SIZE + ICON_GAP;
        } else if (suggestion.fluidIcon() != null) {
            FluidIconRenderer.draw(suggestion.fluidIcon(), x, y - 1, ICON_SIZE, false);
            textX += ICON_SIZE + ICON_GAP;
        }
        int labelWidth = fontRenderer.drawString(label, textX, y + 4, LABEL_COLOR);
        if (!subtitle.isEmpty()) {
            fontRenderer.drawString(
                subtitle,
                textX + labelWidth + fontRenderer.getStringWidth(LABEL_GAP),
                y + 4,
                SUBTITLE_COLOR);
        }
    }
}
