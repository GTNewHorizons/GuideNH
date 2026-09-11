package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

/**
 * Where a markdown snippet belongs in a line.
 *
 * <p>
 * The two constants cover markdown as this mod writes it. A contributor that needs another placement
 * declares its own kind with {@link #of(String)}; kinds compare by value, so a kind declared in two
 * places behaves as one. Snippet detection only asks a kind two questions - whether it starts a line
 * and whether it sits on a word boundary - so a custom kind has to say which of those it wants.
 *
 * @param id       stable identifier of the kind
 * @param lineLead true when the snippet is triggered at the start of a line, false when it applies
 *                 inline within text
 */
public record MarkdownSnippetKind(String id, boolean lineLead) {

    /** Line-leading construct such as a heading, list item, quote, or table. */
    public static final MarkdownSnippetKind BLOCK = new MarkdownSnippetKind("BLOCK", true);
    /** Construct that wraps inline text such as emphasis or a link. */
    public static final MarkdownSnippetKind INLINE = new MarkdownSnippetKind("INLINE", false);

    private static final List<MarkdownSnippetKind> BUILT_IN = List.of(BLOCK, INLINE);

    /** The kinds this mod ships, for tooling and diagnostics. */
    public static List<MarkdownSnippetKind> builtIn() {
        return BUILT_IN;
    }

    /** A line-leading kind owned by another mod. */
    public static MarkdownSnippetKind of(String id) {
        return new MarkdownSnippetKind(id, true);
    }

    /** A kind owned by another mod with an explicit placement. */
    public static MarkdownSnippetKind of(String id, boolean lineLead) {
        return new MarkdownSnippetKind(id, lineLead);
    }

    @Override
    public String toString() {
        return id;
    }
}
