package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.util.Map;

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
    @Getter
    private final Map<String, String> contextValues;

    public FrontmatterContext(String key, boolean isValue, int replaceStart, int replaceEnd, String partialText) {
        this(key, isValue, replaceStart, replaceEnd, partialText, Map.of());
    }

    public FrontmatterContext(String key, boolean isValue, int replaceStart, int replaceEnd, String partialText,
        Map<String, String> contextValues) {
        this.key = key;
        this.isValue = isValue;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
        this.contextValues = Map.copyOf(contextValues);
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

    @Override
    public boolean expandsTypedText() {
        return !isValue;
    }
}
