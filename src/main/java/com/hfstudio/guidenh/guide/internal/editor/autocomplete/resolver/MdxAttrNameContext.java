package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

import lombok.Getter;

public class MdxAttrNameContext implements AutocompleteContext {

    @Getter
    private final String tagName;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;

    public MdxAttrNameContext(String tagName, int replaceStart, int replaceEnd, String partialText) {
        this.tagName = tagName;
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

    /** Accepting an attribute name also writes its value placeholder. */
    @Override
    public boolean expandsTypedText() {
        return true;
    }

    /** '=' ends the attribute name, so it should expand the highlighted name instead of being typed. */
    @Override
    public boolean isCommitCharacter(char typedChar) {
        return typedChar == '=';
    }
}
