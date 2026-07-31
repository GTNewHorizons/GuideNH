package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class TextSyntaxContext {

    @Getter
    private final SyntaxElementType elementType;
    @Getter
    private final int elementStart;
    @Getter
    private final int elementEnd;
    @Nullable
    private final AutocompleteContext autocomplete;

    public TextSyntaxContext(SyntaxElementType elementType, int elementStart, int elementEnd,
        @Nullable AutocompleteContext autocomplete) {
        this.elementType = elementType;
        this.elementStart = elementStart;
        this.elementEnd = elementEnd;
        this.autocomplete = autocomplete;
    }

    @Nullable
    public AutocompleteContext getAutocomplete() {
        return autocomplete;
    }

    public boolean shouldAutocomplete() {
        return autocomplete != null;
    }
}
