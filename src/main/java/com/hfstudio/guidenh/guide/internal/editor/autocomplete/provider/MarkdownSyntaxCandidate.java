package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;

/**
 * Renders a markdown construct with a preview of the text it inserts, and remembers where the caret
 * lands after committing.
 */
public class MarkdownSyntaxCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();
    private static final int PREVIEW_COLOR = ColorUtils.TEXT_DISABLED.getColor();
    private static final String LABEL_GAP = "  ";
    private static final String LINE_BREAK_MARKER = "\\n";
    private static final int MAX_PREVIEW_LENGTH = 24;

    private final MarkdownSnippet snippet;
    private final String preview;

    public MarkdownSyntaxCandidate(MarkdownSnippet snippet) {
        this.snippet = snippet;
        this.preview = summarize(snippet.snippet());
    }

    @Override
    public String displayText() {
        return snippet.label();
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
        return fontRenderer.getStringWidth(snippet.label()) + fontRenderer.getStringWidth(LABEL_GAP)
            + fontRenderer.getStringWidth(preview);
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int labelWidth = fontRenderer.drawString(snippet.label(), x, y + 2, LABEL_COLOR);
        fontRenderer.drawString(preview, x + labelWidth + fontRenderer.getStringWidth(LABEL_GAP), y + 2, PREVIEW_COLOR);
    }

    private static String summarize(String snippet) {
        if (snippet == null || snippet.isEmpty()) {
            return "";
        }
        String flattened = snippet.replace("\r", "")
            .replace("\n", LINE_BREAK_MARKER);
        return flattened.length() > MAX_PREVIEW_LENGTH ? flattened.substring(0, MAX_PREVIEW_LENGTH) + "..." : flattened;
    }
}
