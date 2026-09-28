package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

/**
 * The migration signal for pages that still write brace syntax. Braces are ordinary text after the move to
 * MDX tags, so a leftover call renders literally and this warning is the only thing telling an author why.
 */
class MediaWikiTemplateLegacySyntaxTest {

    @Test
    void findsACallWrittenWithBraces() {
        List<String> found = MediaWikiTemplateDiagnostics.findLegacySyntax("before {{InfoBox|Steel}} after", 3);
        assertEquals(List.of("{{InfoBox|Steel}}"), found);
    }

    @Test
    void findsAParameterWrittenWithTripleBraces() {
        List<String> found = MediaWikiTemplateDiagnostics.findLegacySyntax("Name: {{{1}}}", 3);
        assertEquals(List.of("{{{1}}}"), found);
    }

    @Test
    void findsAParserFunction() {
        List<String> found = MediaWikiTemplateDiagnostics.findLegacySyntax("{{#expr:2 + 2}}", 3);
        assertEquals(List.of("{{#expr:2 + 2}}"), found);
    }

    @Test
    void reportsNothingForTheMdxForm() {
        assertTrue(
            MediaWikiTemplateDiagnostics.findLegacySyntax("<Template name=\"InfoBox\">Steel</Template>", 3)
                .isEmpty());
    }

    @Test
    void reportsNothingForPlainText() {
        assertTrue(
            MediaWikiTemplateDiagnostics.findLegacySyntax("no templates here", 3)
                .isEmpty());
        assertTrue(
            MediaWikiTemplateDiagnostics.findLegacySyntax("", 3)
                .isEmpty());
        assertTrue(
            MediaWikiTemplateDiagnostics.findLegacySyntax(null, 3)
                .isEmpty());
    }

    @Test
    void ignoresASingleBraceOrAnUnclosedPair() {
        assertTrue(find("a { b } c", 3).isEmpty());
        assertTrue(find("{{unclosed", 3).isEmpty());
    }

    @Test
    void ignoresBracesThatSpanLines() {
        assertTrue(find("{{\nname\n}}", 3).isEmpty());
    }

    @Test
    void stopsAtTheLimit() {
        List<String> found = MediaWikiTemplateDiagnostics.findLegacySyntax("{{A}} {{B}} {{C}} {{D}}", 2);
        assertEquals(2, found.size());
    }

    @Test
    void formatsAWarningThatNamesThePageAndReplacement() {
        List<String> lines = MediaWikiTemplateDiagnostics.formatIssues(
            new ResourceLocation("guidenh", "guidenh/page.md"),
            List.of(new MediaWikiTemplateIssue(MediaWikiTemplateIssueKind.UNKNOWN_TEMPLATE, "Unknown template X")));
        assertFalse(lines.isEmpty());
        assertTrue(
            lines.get(0)
                .contains("guidenh/page.md"),
            () -> lines.get(0));
    }

    /** Shorthand so the cases that expect nothing stay one line each. */
    private static List<String> find(String source, int limit) {
        return MediaWikiTemplateDiagnostics.findLegacySyntax(source, limit);
    }
}
