package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

/** One entry of the completion popup. */
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

    /** Caret position counted from the start of {@link #replacementText()} once the candidate is committed. */
    default int caretOffsetInReplacement() {
        return -1;
    }

    /** Selection end counted from the start of {@link #replacementText()}. */
    default int selectionEndInReplacement() {
        return -1;
    }

    /** Text written after the caret, so one candidate can produce a pair such as a closing tag. */
    @Nullable
    default String suffixText() {
        return null;
    }

    /** True when the value must be quoted unless the surrounding page already quotes it. */
    default boolean quotesValue() {
        return false;
    }

    /** The suggestion this candidate stands for, when it wraps one. */
    @Nullable
    default SyntaxSuggestion syntaxSuggestion() {
        return null;
    }

    /** The text this candidate is ranked by, which is what the author is typing towards. */
    default String rankingText() {
        return replacementText();
    }

    void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered);
}
