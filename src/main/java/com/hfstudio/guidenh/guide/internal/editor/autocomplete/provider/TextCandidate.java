package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.color.ColorUtils;

public class TextCandidate implements AutocompleteCandidate {

    private final String text;
    private final String display;
    private static final int TEXT_COLOR = ColorUtils.TEXT.getColor();

    public TextCandidate(String text) {
        this.text = text;
        this.display = CompletionPreview.summarize(text);
    }

    @Override
    public String displayText() {
        return display;
    }

    @Override
    public String replacementText() {
        return text;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        fontRenderer.drawString(display, x, y + 2, TEXT_COLOR);
    }
}
