package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import lombok.Getter;

@Getter
public class AutocompleteCommit {

    private final String text;
    private final int selectionStart;
    private final int selectionEnd;

    public AutocompleteCommit(String text, int selectionStart, int selectionEnd) {
        this.text = text;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

}
