package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;

/**
 * Renders a markdown construct with a preview of the text it inserts, and remembers where the caret lands after.
 */
public class MarkdownSyntaxCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int PREVIEW_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final String LABEL_GAP = "  ";
    private static final int MAX_PREVIEW_LENGTH = 24;

    private final MarkdownSnippet snippet;
    private final String label;
    private final String preview;

    public MarkdownSyntaxCandidate(MarkdownSnippet snippet) {
        this.snippet = snippet;
        this.label = CompletionPreview.summarize(snippet.label());
        this.preview = CompletionPreview.summarize(snippet.snippet(), MAX_PREVIEW_LENGTH);
    }

    @Override
    public String displayText() {
        return label;
    }

    @Override
    public String replacementText() {
        return snippet.snippet();
    }

    @Override
    public int caretOffsetInReplacement() {
        return snippet.caretOffset();
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        return fontRenderer.getStringWidth(label) + fontRenderer.getStringWidth(LABEL_GAP)
            + fontRenderer.getStringWidth(preview);
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int labelWidth = fontRenderer.drawString(label, x, y + 2, LABEL_COLOR);
        fontRenderer.drawString(preview, x + labelWidth + fontRenderer.getStringWidth(LABEL_GAP), y + 2, PREVIEW_COLOR);
    }

}
