package com.hfstudio.guidenh.guide.syntax;

public interface SyntaxSink {

    SyntaxSink tags(String... names);

    SyntaxSink containerTags(String... names);

    SyntaxSink children(String containerTag, String... childTags);

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
