package com.hfstudio.guidenh.guide.syntax;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * One attribute of a tag: its name, the kind of value it expects, and optionally a fixed set of
 * suggestions. Attributes with a fixed set do not need a {@link SyntaxValueSource}.
 *
 * @param name        attribute name as written in the page
 * @param kind        value kind, which selects the value source used for completion
 * @param suggestions fixed values to offer; empty for kinds answered by a value source
 * @param enumType    an enum whose constants are offered when {@code kind} is
 *                    {@link SyntaxValueKind#ENUM} and no fixed values are given
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
