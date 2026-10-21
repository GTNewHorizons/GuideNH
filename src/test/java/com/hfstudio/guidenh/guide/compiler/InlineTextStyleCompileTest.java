package com.hfstudio.guidenh.guide.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;
import com.hfstudio.guidenh.guide.style.TextStyle;

class InlineTextStyleCompileTest {

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @Test
    void boldMarkdownInsideAnOrderedListItemIsStyled() {
        String source = "1. **Text style should Applied here** asdf asdf\n";
        assertEquals(
            List.of("Text style should Applied here"),
            styledText(source, style -> Boolean.TRUE.equals(style.bold())));
        assertFalse(text(source).contains("**"));
    }

    @Test
    void htmlBoldAndItalicCompileLikeTheirMarkdownForms() {
        String source = "1. <b>bold</b> and <i>italic</i>\n";
        assertEquals(List.of("bold"), styledText(source, style -> Boolean.TRUE.equals(style.bold())));
        assertEquals(List.of("italic"), styledText(source, style -> Boolean.TRUE.equals(style.italic())));
        assertFalse(text(source).contains("Unhandled"));
    }

    private String text(String source) {
        return MediaWikiTemplateTestHarness.text(harness.compile(source));
    }

    private List<String> styledText(String source, Predicate<TextStyle> matches) {
        List<String> found = new ArrayList<>();
        harness.compile(source)
            .document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeFlowContent(LytFlowContent content) {
                    if (content instanceof LytFlowSpan span && matches.test(span.getStyle())) {
                        StringBuilder text = new StringBuilder();
                        span.visit(new LytVisitor() {

                            @Override
                            public void text(String value) {
                                text.append(value);
                            }
                        });
                        found.add(text.toString());
                        return Result.SKIP_CHILDREN;
                    }
                    return Result.CONTINUE;
                }
            });
        return found;
    }
}
