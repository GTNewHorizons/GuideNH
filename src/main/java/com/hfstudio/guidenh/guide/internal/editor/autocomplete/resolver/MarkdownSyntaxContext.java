package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippetKind;

import lombok.Getter;

/** Carries the markdown construct region the cursor sits in. */
public class MarkdownSyntaxContext implements AutocompleteContext {

    @Getter
    private final MarkdownSnippetKind kind;
    private final int replaceStart;
    private final int replaceEnd;
    private final String partialText;

    public MarkdownSyntaxContext(MarkdownSnippetKind kind, int replaceStart, int replaceEnd, String partialText) {
        this.kind = kind;
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
