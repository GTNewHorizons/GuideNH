package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * One attribute of a tag: its name, the kind of value it expects, and optionally a fixed set of
 * suggestions. Attributes with a fixed set do not need a {@link SyntaxValueSource}.
 */
public record AttributeSyntax(String name, SyntaxValueKind kind, List<String> suggestions,
    @Nullable Class<? extends Enum<?>> enumType) {

    public AttributeSyntax {
        suggestions = List.copyOf(suggestions);
    }

    public static AttributeSyntax of(String name, SyntaxValueKind kind) {
        return new AttributeSyntax(name, kind, List.of(), null);
    }

    public static AttributeSyntax of(String name, SyntaxValueKind kind, String... suggestions) {
        return new AttributeSyntax(name, kind, List.of(suggestions), null);
    }

    public static AttributeSyntax of(String name, SyntaxValueKind kind, Class<? extends Enum<?>> enumType) {
        return new AttributeSyntax(name, kind, List.of(), enumType);
    }

}
