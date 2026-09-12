package com.hfstudio.guidenh.guide.syntax;

/** The registration surface handed to a {@link SyntaxContributor}. */
public interface SyntaxSink {

    SyntaxSink tags(String... names);

    SyntaxSink containerTags(String... names);

    SyntaxSink children(String containerTag, String... childTags);

    /** Hides tags from completion. */
    SyntaxSink hiddenTags(String... names);

    SyntaxSink attributes(String tagName, AttributeSyntax... attributes);

    /** Declares the text a tag completes as, for tags whose useful form is more than the tag name. */
    SyntaxSink insertTemplates(InsertTemplate... templates);

    SyntaxSink markdown(MarkdownSnippet... snippets);

    SyntaxSink fenceLanguages(String... names);

    SyntaxSink frontmatterKeys(String... keys);

    SyntaxSink frontmatterValues(String key, String... values);

    SyntaxSink frontmatterKind(String key, SyntaxValueKind kind);

    SyntaxSink valueSource(SyntaxValueSource source);
}
