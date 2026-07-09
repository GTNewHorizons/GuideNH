package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

import lombok.Getter;

/** Replaces the old MdxAutocompleteContext. Carries tag name, attribute name, and replacement range. */
public class MdxValueContext implements AutocompleteContext {

    @Getter
    private final String tagName;
    @Getter
    private final String attrName;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;
    @Getter
    private final char missingValueTerminator;

    public MdxValueContext(String tagName, String attrName, int replaceStart, int replaceEnd, String partialText) {
        this(tagName, attrName, replaceStart, replaceEnd, partialText, '\0');
    }

    public MdxValueContext(String tagName, String attrName, int replaceStart, int replaceEnd, String partialText,
        char missingValueTerminator) {
        this.tagName = tagName;
        this.attrName = attrName;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.partialText = partialText;
        this.missingValueTerminator = missingValueTerminator;
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
