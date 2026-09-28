package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

import lombok.Getter;

public class MdxAttrNameContext implements AutocompleteContext {

    @Getter
    private final String tagName;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;
    @Getter
    private final String templateName;

    public MdxAttrNameContext(String tagName, int replaceStart, int replaceEnd, String partialText) {
        this(tagName, replaceStart, replaceEnd, partialText, null);
    }

    public MdxAttrNameContext(String tagName, int replaceStart, int replaceEnd, String partialText,
        String templateName) {
        this.tagName = tagName;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
        this.templateName = templateName;
    }

    public boolean hasTemplate() {
        return templateName != null && !templateName.isBlank();
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
        return true;
    }

    @Override
    public boolean isCommitCharacter(char typedChar) {
        return typedChar == '=';
    }
}
