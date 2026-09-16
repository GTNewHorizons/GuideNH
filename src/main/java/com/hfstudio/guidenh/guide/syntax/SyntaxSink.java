package com.hfstudio.guidenh.guide.syntax;

public interface SyntaxSink {

    SyntaxSink tags(String... names);

    SyntaxSink containerTags(String... names);

    SyntaxSink children(String containerTag, String... childTags);

    /**
     * Declares the tags worth suggesting first inside a container whose body also takes ordinary block
     * content, such as an annotation tooltip or a {@code <details>} body. Unlike {@link #children} the list is
     * a suggestion order rather than a restriction: validation accepts any block tag there, while completion
     * still offers these first.
     */
    SyntaxSink preferredChildren(String containerTag, String... childTags);

    SyntaxSink hiddenTags(String... names);

    SyntaxSink attributes(String tagName, AttributeSyntax... attributes);

    SyntaxSink insertTemplates(InsertTemplate... templates);

    SyntaxSink markdown(MarkdownSnippet... snippets);

    SyntaxSink fenceLanguages(String... names);

    SyntaxSink frontmatterKeys(String... keys);

    SyntaxSink frontmatterValues(String key, String... values);

    SyntaxSink frontmatterKind(String key, SyntaxValueKind kind);

    SyntaxSink valueSource(SyntaxValueSource source);
}
