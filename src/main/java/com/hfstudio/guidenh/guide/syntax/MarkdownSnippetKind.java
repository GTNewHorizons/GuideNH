package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

/** Where a markdown snippet belongs in a line. */
public record MarkdownSnippetKind(String id, boolean lineLead) {

    public static final MarkdownSnippetKind BLOCK = new MarkdownSnippetKind("BLOCK", true);
    public static final MarkdownSnippetKind INLINE = new MarkdownSnippetKind("INLINE", false);

    private static final List<MarkdownSnippetKind> BUILT_IN = List.of(BLOCK, INLINE);

    public static List<MarkdownSnippetKind> builtIn() {
        return BUILT_IN;
    }

    public static MarkdownSnippetKind of(String id) {
        return new MarkdownSnippetKind(id, true);
    }

    public static MarkdownSnippetKind of(String id, boolean lineLead) {
        return new MarkdownSnippetKind(id, lineLead);
    }

    @Override
    public String toString() {
        return id;
    }
}
