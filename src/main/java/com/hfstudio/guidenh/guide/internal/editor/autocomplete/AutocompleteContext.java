package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

/**
 * Describes the syntactical slot the caret sits in: which range a candidate replaces, what has been
 * typed inside it, and how accepting a candidate behaves in that slot.
 */
public interface AutocompleteContext {

    int replaceStart();

    int replaceEnd();

    String getPartialText();

    /**
     * True when accepting any candidate inserts markup around the typed text, so a candidate whose
     * name already equals what was typed is still worth offering. For example an attribute name
     * expands to {@code name=""} and a frontmatter key expands to {@code key: }.
     */
    default boolean expandsTypedText() {
        return false;
    }

    /**
     * True when {@code typedChar} ends this slot, so accepting the highlighted candidate produces a
     * better result than inserting the character literally. The character is consumed, not typed.
     */
    default boolean isCommitCharacter(char typedChar) {
        return false;
    }
}
