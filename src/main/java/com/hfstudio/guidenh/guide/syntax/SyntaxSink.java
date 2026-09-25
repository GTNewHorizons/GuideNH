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

    /**
     * Declares that a tag forwards every attribute it is given, so an attribute not named by
     * {@link #attributes} is still valid. A {@code <Template>} call is the case this exists for: any
     * attribute other than the first {@code name} becomes an argument, so the accepted set cannot be
     * enumerated. Known attributes remain typed and are still checked; this only stops the rest being
     * reported as unknown.
     */
    SyntaxSink forwardsAttributes(String... tagNames);

    SyntaxSink insertTemplates(InsertTemplate... templates);

    SyntaxSink markdown(MarkdownSnippet... snippets);

    SyntaxSink fenceLanguages(String... names);

    SyntaxSink frontmatterKeys(String... keys);

    SyntaxSink frontmatterValues(String key, String... values);

    SyntaxSink frontmatterKind(String key, SyntaxValueKind kind);

    SyntaxSink valueSource(SyntaxValueSource source);
}
