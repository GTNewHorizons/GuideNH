package com.hfstudio.guidenh.guide.syntax;

/**
 * The registration surface handed to a {@link SyntaxContributor}. Every method returns {@code this}
 * so a contributor reads as one chained declaration.
 */
public interface SyntaxSink {

    /** Declares self-closing tags that wrap no content. */
    SyntaxSink tags(String... names);

    /** Declares tags that wrap content and are completed as {@code <Name></Name>}. */
    SyntaxSink containerTags(String... names);

    /** Declares which tags may appear directly inside {@code containerTag}. */
    SyntaxSink children(String containerTag, String... childTags);

    /**
     * Hides tags from completion. Use it for tags a compiler accepts but authors never write, such as
     * the tags the markdown parser produces internally.
     */
    SyntaxSink hiddenTags(String... names);

    /** Declares the attributes of a tag. Re-declaring a name replaces its earlier declaration. */
    SyntaxSink attributes(String tagName, AttributeSyntax... attributes);

    /**
     * Declares the text a tag completes as, for tags whose useful form is more than the tag name.
     * Re-declaring a tag replaces its earlier template.
     */
    SyntaxSink insertTemplates(InsertTemplate... templates);

    /** Declares markdown snippets. */
    SyntaxSink markdown(MarkdownSnippet... snippets);

    /** Declares fence names usable on a code block. */
    SyntaxSink fenceLanguages(String... names);

    /** Declares frontmatter keys. */
    SyntaxSink frontmatterKeys(String... keys);

    /** Declares the fixed values of a frontmatter key. */
    SyntaxSink frontmatterValues(String key, String... values);

    /** Declares the value kind of a frontmatter key, so a value source answers for it. */
    SyntaxSink frontmatterKind(String key, SyntaxValueKind kind);

    /** Registers a source that answers completion requests for one or more value kinds. */
    SyntaxSink valueSource(SyntaxValueSource source);
}
