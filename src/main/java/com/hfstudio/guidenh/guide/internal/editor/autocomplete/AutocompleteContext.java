package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

/**
 * Describes the syntactical slot the caret sits in: which range a candidate replaces, what has been typed inside.
 */
public interface AutocompleteContext {

    int replaceStart();

    int replaceEnd();

    String getPartialText();

    /** True when accepting any candidate inserts markup around the typed text, so a candidate whose name already. */
    default boolean expandsTypedText() {
        return false;
    }

    /** True when {@code typedChar} ends this slot, so accepting the highlighted candidate produces a better result. */
    default boolean isCommitCharacter(char typedChar) {
        return false;
    }
}
