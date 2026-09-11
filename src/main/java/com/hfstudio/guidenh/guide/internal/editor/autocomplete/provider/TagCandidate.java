package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;

/**
 * A tag suggestion. Container tags close themselves around the caret, self-closing tags get the
 * {@code />} tail from {@code AutocompleteCommitService}. Whether a tag wraps content comes from the
 * guide's syntax model, not from this class.
 */
public class TagCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();

    private final String tagName;
    private final boolean container;

    public TagCandidate(String tagName, boolean container) {
        this.tagName = tagName;
        this.container = container;
    }

    @Override
    public String displayText() {
        return container ? tagName + "  </" + tagName + ">" : tagName;
    }

    @Override
    public String replacementText() {
        return container ? tagName + ">" : tagName;
    }

    @Nullable
    @Override
    public String suffixText() {
        return container ? "</" + tagName + ">" : null;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        fontRenderer.drawString(displayText(), x, y + 2, LABEL_COLOR);
    }
}
