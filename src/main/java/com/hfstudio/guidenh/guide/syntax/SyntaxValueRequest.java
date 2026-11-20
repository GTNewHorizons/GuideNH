package com.hfstudio.guidenh.guide.syntax;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public record SyntaxValueRequest(SyntaxValueKind kind, String partialText, @Nullable String tagName,
    @Nullable String attributeName, @Nullable String frontmatterKey, Map<String, String> contextValues) {

    public SyntaxValueRequest {
        contextValues = Map.copyOf(contextValues);
    }

    public SyntaxValueRequest(SyntaxValueKind kind, String partialText, @Nullable String tagName,
        @Nullable String attributeName, @Nullable String frontmatterKey) {
        this(kind, partialText, tagName, attributeName, frontmatterKey, Map.of());
    }

    public SyntaxValueRequest withContextValues(Map<String, String> values) {
        return new SyntaxValueRequest(kind, partialText, tagName, attributeName, frontmatterKey, values);
    }

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
