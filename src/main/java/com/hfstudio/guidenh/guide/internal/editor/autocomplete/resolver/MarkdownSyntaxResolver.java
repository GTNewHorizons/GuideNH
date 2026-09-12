package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;
import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippet;
import com.hfstudio.guidenh.guide.syntax.MarkdownSnippetKind;

/** Detects markdown constructs that are about to be typed. */
public class MarkdownSyntaxResolver implements SyntaxContextResolver {

    private static final int MAX_BLOCK_INDENT = 8;

    private GuideSyntaxModel model = GuideSyntaxModel.empty();

    public void setModel(@Nullable GuideSyntaxModel model) {
        this.model = model != null ? model : GuideSyntaxModel.empty();
    }

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty() || cursorIndex < 0 || cursorIndex > text.length()) {
            return null;
        }
        TextSyntaxContext block = resolveBlock(text, cursorIndex);
        if (block != null) {
            return block;
        }
        return resolveInline(text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveBlock(String text, int cursorIndex) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int contentStart = lineStart;
        while (contentStart < cursorIndex && text.charAt(contentStart) == ' ') {
            contentStart++;
        }
        if (contentStart - lineStart > MAX_BLOCK_INDENT || contentStart >= cursorIndex) {
            return null;
        }
        String typed = text.substring(contentStart, cursorIndex);
        if (!MarkdownSnippet.anyMatches(model.markdownSnippets(), typed)) {
            return null;
        }
        return new TextSyntaxContext(
            SyntaxElementType.MARKDOWN_SYNTAX,
            contentStart,
            cursorIndex,
            new MarkdownSyntaxContext(MarkdownSnippetKind.BLOCK, contentStart, cursorIndex, typed));
    }

    @Nullable
    private TextSyntaxContext resolveInline(String text, int cursorIndex) {
        MarkdownSnippet snippet = MarkdownSnippet.longestInlineTrigger(model.markdownSnippets(), text, cursorIndex);
        if (snippet == null) {
            return null;
        }
        int replaceStart = cursorIndex - snippet.trigger()
            .length();
        return new TextSyntaxContext(
            SyntaxElementType.MARKDOWN_SYNTAX,
            replaceStart,
            cursorIndex,
            new MarkdownSyntaxContext(MarkdownSnippetKind.INLINE, replaceStart, cursorIndex, snippet.trigger()));
    }
}
