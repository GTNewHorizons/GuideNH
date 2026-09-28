package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;

/**
 * The inline {@code &[label](sound:...)} action. It is a Markdown link with a leading {@code &}, so the
 * parser consumes the bracketed part and leaves a stray ampersand unless the whole form is taken out of the
 * source before parsing.
 */
class MarkdownActionLinkTest {

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @AfterEach
    void clear() {
        MediaWikiTemplateRepository.clear();
        MediaWikiTemplateDependencyGraph.clear();
    }

    @Test
    void maskingTakesTheWholeFormOutOfTheSource() {
        String source = "Text &[click me](sound:guidenh:guide.sample_click) after";
        MarkdownActionLink.MaskResult mask = MarkdownActionLink.mask(source);

        assertFalse(mask.isEmpty(), "the action must be masked");
        assertFalse(
            mask.source()
                .contains("[click me]"),
            "the bracketed part must not survive for the parser to read as a link");
        assertTrue(
            mask.source()
                .contains("Text "),
            "the surrounding text must be left alone");
        assertTrue(
            mask.source()
                .endsWith(" after"),
            "the surrounding text must be left alone");
    }

    @Test
    void maskingIsSkippedWhenNothingMatches() {
        MarkdownActionLink.MaskResult mask = MarkdownActionLink.mask("plain text without actions");

        assertTrue(mask.isEmpty());
        assertEquals("plain text without actions", mask.source());
    }

    @Test
    void theLabelRendersWithoutTheStrayAmpersandOrUri() {
        String rendered = MediaWikiTemplateTestHarness
            .text(harness.compile("&[Inline sound action](sound:guidenh:guide.sample_click)\n"));

        assertEquals("Inline sound action", rendered.trim());
        assertFalse(rendered.contains("&["), () -> rendered);
        assertFalse(rendered.contains("sound:"), () -> rendered);
    }

    @Test
    void anActionInsideTextKeepsTheSurroundingText() {
        String rendered = MediaWikiTemplateTestHarness
            .text(harness.compile("before &[click](sound:guidenh:guide.sample_click) after\n"));

        assertTrue(rendered.contains("before"), () -> rendered);
        assertTrue(rendered.contains("click"), () -> rendered);
        assertTrue(rendered.contains("after"), () -> rendered);
        assertFalse(rendered.contains("&["), () -> rendered);
    }

    @Test
    void aLinkWithoutASoundUriIsLeftAsWritten() {
        String rendered = MediaWikiTemplateTestHarness.text(harness.compile("&[label](guidenh:page.md)\n"));

        assertTrue(rendered.contains("&[label](guidenh:page.md)"), () -> rendered);
    }

    @Test
    void splitStillReadsTheLabelAndUri() {
        List<MarkdownActionLink.Segment> segments = MarkdownActionLink
            .split("&[Inline sound action](sound:guidenh:guide.sample_click)");

        assertEquals(1, segments.size());
        assertTrue(
            segments.get(0)
                .isLink());
        assertEquals(
            "Inline sound action",
            segments.get(0)
                .text());
        assertEquals(
            "sound:guidenh:guide.sample_click",
            segments.get(0)
                .href());
    }
}
