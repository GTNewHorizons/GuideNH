package com.hfstudio.guidenh.guide.syntax;

import org.jetbrains.annotations.Nullable;

/**
 * A completion request routed to a {@link SyntaxValueSource}.
 *
 * @param kind           the value kind being completed
 * @param partialText    what the user typed so far, without any surrounding quotes
 * @param tagName        enclosing tag, or null outside a tag
 * @param attributeName  attribute being completed, or null outside an attribute value
 * @param frontmatterKey frontmatter key being completed, or null outside the frontmatter
 */
public record SyntaxValueRequest(SyntaxValueKind kind, String partialText, @Nullable String tagName,
    @Nullable String attributeName, @Nullable String frontmatterKey) {

    public static SyntaxValueRequest of(SyntaxValueKind kind, @Nullable String partialText) {
        return new SyntaxValueRequest(kind, partialText != null ? partialText : "", null, null, null);
    }

    public static SyntaxValueRequest of(SyntaxValueKind kind, @Nullable String partialText, @Nullable String tagName,
        @Nullable String attributeName) {
        return new SyntaxValueRequest(kind, partialText != null ? partialText : "", tagName, attributeName, null);
    }

    public static SyntaxValueRequest frontmatter(SyntaxValueKind kind, @Nullable String partialText,
        @Nullable String frontmatterKey) {
        return new SyntaxValueRequest(kind, partialText != null ? partialText : "", null, null, frontmatterKey);
    }
}
