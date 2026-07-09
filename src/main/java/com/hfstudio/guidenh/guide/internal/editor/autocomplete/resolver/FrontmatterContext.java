package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

import lombok.Getter;

public class FrontmatterContext implements AutocompleteContext {

    @Getter
    private final String key;
    @Getter
    private final boolean isValue;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;

    public FrontmatterContext(String key, boolean isValue, int replaceStart, int replaceEnd, String partialText) {
        this.key = key;
        this.isValue = isValue;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
    }

    @Override
    public int replaceStart() {
        return replaceStart;
    }

    @Override
    public int replaceEnd() {
        return replaceEnd;
    }

    @Override
    public String getPartialText() {
        return partialText;
    }
}
