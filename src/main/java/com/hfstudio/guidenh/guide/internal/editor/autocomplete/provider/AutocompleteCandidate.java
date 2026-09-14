package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

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

    default int caretOffsetInReplacement() {
        return -1;
    }

    default int selectionEndInReplacement() {
        return -1;
    }

    @Nullable
    default String suffixText() {
        return null;
    }

    default boolean quotesValue() {
        return false;
    }

    @Nullable
    default SyntaxSuggestion syntaxSuggestion() {
        return null;
    }

    default String rankingText() {
        return replacementText();
    }

    void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered);
}
