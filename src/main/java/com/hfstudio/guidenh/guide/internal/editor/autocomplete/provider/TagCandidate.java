package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.syntax.InsertTemplate;

/**
 * A tag suggestion. Container tags close themselves around the caret, self-closing tags get the
 * {@code />} tail from {@code AutocompleteCommitService}, and a tag with an
 * {@link InsertTemplate} writes the form the contributor declared instead.
 *
 * <p>
 * Whether a tag wraps content and which template it declares come from the guide's syntax model, not
 * from this class.
 */
public class TagCandidate implements AutocompleteCandidate {

    private static final int LABEL_COLOR = ColorUtils.TEXT.getColor();

    private final String tagName;
    private final boolean container;
    @Nullable
    private final InsertTemplate template;
    private final String display;

    public TagCandidate(String tagName, boolean container) {
        this(tagName, container, null);
    }

    public TagCandidate(String tagName, boolean container, @Nullable InsertTemplate template) {
        this.tagName = tagName;
        this.container = container;
        this.template = template;
        this.display = buildDisplay(tagName, container, template);
    }

    /** A template spans several lines, so the popup row shows it as one. */
    private static String buildDisplay(String tagName, boolean container, @Nullable InsertTemplate template) {
        if (template != null) {
            return template.text()
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        }
        return container ? tagName + "  </" + tagName + ">" : tagName;
    }

    @Override
    public String displayText() {
        return display;
    }

    @Override
    public String replacementText() {
        if (template != null) {
            return template.text();
        }
        return container ? tagName + ">" : tagName;
    }

    @Override
    public String rankingText() {
        // A template writes the whole tag, but the author is typing the tag name.
        return tagName;
    }

    @Override
    public int caretOffsetInReplacement() {
        return template != null ? template.caretOffset() : -1;
    }

    @Override
    public int selectionEndInReplacement() {
        return template != null ? template.caretOffset() : -1;
    }

    @Nullable
    @Override
    public String suffixText() {
        if (template != null) {
            return null;
        }
        return container ? "</" + tagName + ">" : null;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        fontRenderer.drawString(display, x, y + 2, LABEL_COLOR);
    }
}
