package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

/**
 * One entry of the completion popup. A candidate describes a snippet: {@link #replacementText()} is
 * written over the typed text, {@link #caretOffsetInReplacement()} places the caret inside it,
 * {@link #selectionEndInReplacement()} optionally selects part of it, and {@link #suffixText()} is
 * appended after the caret (a closing tag, for instance).
 */
public interface AutocompleteCandidate {

    String displayText();

    String replacementText();

    default int renderHeight() {
        return 14;
    }

    /** Width hint for popup sizing. Default 0 means use displayText width. */
    default int renderWidth(FontRenderer fontRenderer) {
        return 0;
    }

    /**
     * Caret position counted from the start of {@link #replacementText()} once the candidate is
     * committed. A negative value keeps the caret at the end of the replacement.
     */
    default int caretOffsetInReplacement() {
        return -1;
    }

    /**
     * Selection end counted from the start of {@link #replacementText()}. A negative value leaves the
     * caret without a selection.
     */
    default int selectionEndInReplacement() {
        return -1;
    }

    /**
     * Text written directly after the caret, so a single candidate can produce a paired construct
     * such as {@code <Row>|</Row>}. Null means nothing is appended.
     */
    @Nullable
    default String suffixText() {
        return null;
    }

    /**
     * True when the value must be quoted unless the surrounding page already quotes it. Attribute
     * value candidates derive this from their {@code SyntaxValueKind}.
     */
    default boolean quotesValue() {
        return false;
    }

    /**
     * The suggestion this candidate stands for, when it wraps one. A slot of another mod writes its own
     * replacement, so it needs the value the author picked rather than the rendered text.
     */
    @Nullable
    default SyntaxSuggestion syntaxSuggestion() {
        return null;
    }

    /**
     * The text this candidate is ranked by, which is what the author is typing towards. A candidate whose
     * written form is longer than that, such as a whole tag template, ranks on the name it stands for.
     */
    default String rankingText() {
        return replacementText();
    }

    void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered);
}
