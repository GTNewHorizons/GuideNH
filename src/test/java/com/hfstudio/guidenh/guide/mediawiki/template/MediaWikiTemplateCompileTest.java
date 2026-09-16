package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * End-to-end behaviour of the MDX template layer, exercised through the real page compiler.
 */
class MediaWikiTemplateCompileTest {

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @AfterEach
    void clear() {
        MediaWikiTemplateRepository.clear();
        MediaWikiTemplateDependencyGraph.clear();
    }

    @Test
    void rendersAParameterByName() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\" />");
        assertEquals("Name: Steel", text("<Template name=\"Box\"><Arg name=\"name\">Steel</Arg></Template>"));
    }

    @Test
    void readsArgumentsWrittenOnTheirOwnLines() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\" />");
        // One argument per line makes the parser wrap them in a paragraph, so this is the form that once lost
        // every argument and silently fell back to the defaults.
        assertEquals("Name: Steel", text("<Template name=\"Box\">\n  <Arg name=\"name\">Steel</Arg>\n</Template>"));
    }

    @Test
    void readsPositionalArgumentsWrittenOnTheirOwnLines() {
        harness.defineTemplate("Box", "Name: <Param pos=\"1\" />");
        assertEquals("Name: Steel", text("<Template name=\"Box\">\n  <Arg>Steel</Arg>\n</Template>"));
    }

    @Test
    void aSecondNameAttributeIsAnArgument() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\" default=\"Untitled\" />");
        // The first `name` selects the template, so the second one has to reach the `name` parameter.
        assertEquals("Name: Gold", text("<Template name=\"Box\" name=\"Gold\" />"));
    }

    @Test
    void rendersAParameterByPosition() {
        harness.defineTemplate("Box", "Name: <Param pos=\"1\" />");
        assertEquals("Name: Steel", text("<Template name=\"Box\"><Arg>Steel</Arg></Template>"));
    }

    @Test
    void attributeIsShorthandForAnArgument() {
        harness.defineTemplate("Box", "Name: <Param name=\"label\" />");
        assertEquals("Name: Steel", text("<Template name=\"Box\" label=\"Steel\" />"));
    }

    @Test
    void usesTheDefaultWhenNoArgumentIsGiven() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\" default=\"Untitled\" />");
        assertEquals("Name: Untitled", text("<Template name=\"Box\" />"));
    }

    @Test
    void keepsParameterBodyAsPlaceholderText() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\">Untitled</Param>");
        assertEquals("Name: Untitled", text("<Template name=\"Box\" />"));
    }

    @Test
    void argumentOverridesTheDefault() {
        harness.defineTemplate("Box", "Name: <Param name=\"name\" default=\"Untitled\" />");
        assertEquals("Name: Steel", text("<Template name=\"Box\"><Arg name=\"name\">Steel</Arg></Template>"));
    }

    @Test
    void ifTakesThenBranchWhenTheValueIsPresent() {
        harness.defineTemplate("T", "<If test=\"y\">yes<Else />no</If>");
        assertEquals("yes", text("<Template name=\"T\" y=\"1\" />"));
    }

    @Test
    void ifTakesElseBranchWhenTheValueIsAbsent() {
        harness.defineTemplate("T", "<If test=\"y\">yes<Else />no</If>");
        assertEquals("no", text("<Template name=\"T\" />"));
    }

    @Test
    void ifEqComparesValues() {
        harness.defineTemplate("T", "<IfEq a=\"x\" b=\"mv\">same<Else />other</IfEq>");
        assertEquals("same", text("<Template name=\"T\" x=\"mv\" />"));
    }

    @Test
    void ifEqComparesNumericallyWhenBothLookNumeric() {
        harness.defineTemplate("T", "<IfEq a=\"x\" b=\"1.0\">same<Else />other</IfEq>");
        assertEquals("same", text("<Template name=\"T\" x=\"1\" />"));
    }

    @Test
    void switchSelectsTheMatchingCase() {
        harness
            .defineTemplate("T", "<Switch test=\"tier\"><Case value=\"mv\">MV</Case><Default>none</Default></Switch>");
        assertEquals("MV", text("<Template name=\"T\" tier=\"mv\" />"));
    }

    @Test
    void switchFallsBackToDefault() {
        harness
            .defineTemplate("T", "<Switch test=\"tier\"><Case value=\"mv\">MV</Case><Default>none</Default></Switch>");
        assertEquals("none", text("<Template name=\"T\" tier=\"zzz\" />"));
    }

    @Test
    void exprEvaluatesArithmeticWithPrecedence() {
        harness.defineTemplate("T", "<Expr value=\"2 + 3 * 4\" />");
        assertEquals("14", text("<Template name=\"T\" />"));
    }

    @Test
    void exprEvaluatesAComparison() {
        harness.defineTemplate("T", "<Expr value=\"7 &gt; 5\" />");
        assertEquals("1", text("<Template name=\"T\" />"));
    }

    @Test
    void lenCountsCharacters() {
        harness.defineTemplate("T", "<Len value=\"abc\" />");
        assertEquals("3", text("<Template name=\"T\" />"));
    }

    @Test
    void upperAndLowerChangeCase() {
        harness.defineTemplate("T", "<Upper value=\"abc\" />");
        assertEquals("ABC", text("<Template name=\"T\" />"));
    }

    @Test
    void subExtractsARange() {
        harness.defineTemplate("T", "<Sub value=\"abcdef\" start=\"1\" length=\"3\" />");
        assertEquals("bcd", text("<Template name=\"T\" />"));
    }

    @Test
    void replaceSubstitutesText() {
        harness.defineTemplate("T", "<Replace value=\"a-b\" from=\"-\" to=\"+\" />");
        assertEquals("a+b", text("<Template name=\"T\" />"));
    }

    @Test
    void urlEncodeEscapesSpacesAsPercentTwenty() {
        harness.defineTemplate("T", "<UrlEncode value=\"a b\" />");
        assertEquals("a%20b", text("<Template name=\"T\" />"));
    }

    @Test
    void posFindsTheFirstOccurrence() {
        harness.defineTemplate("T", "<Pos value=\"abc\" needle=\"c\" />");
        assertEquals("2", text("<Template name=\"T\" />"));
    }

    @Test
    void noIncludeIsDroppedOnTransclusion() {
        harness.defineTemplate("T", "body<NoInclude>docs</NoInclude>");
        assertEquals("body", text("<Template name=\"T\" />"));
    }

    @Test
    void includeOnlyIsKeptWithoutItsWrapper() {
        harness.defineTemplate("T", "<IncludeOnly>kept</IncludeOnly>");
        assertEquals("kept", text("<Template name=\"T\" />"));
    }

    @Test
    void onlyIncludeNarrowsTheBody() {
        harness.defineTemplate("T", "A<NoInclude>B</NoInclude>C<OnlyInclude>only</OnlyInclude>D");
        assertEquals("only", text("<Template name=\"T\" />"));
    }

    @Test
    void templateFrontmatterIsNotTranscluded() {
        harness.defineTemplate("T", "---\nnavigation:\n  title: Leaked\n---\n\nreal body");
        String text = text("<Template name=\"T\" />");
        assertTrue(text.contains("real body"), () -> text);
        assertFalse(text.contains("Leaked"), () -> text);
    }

    @Test
    void nestedTemplateIsIncluded() {
        harness.defineTemplate("Inner", "inner");
        harness.defineTemplate("Outer", "outer(<Template name=\"Inner\" />)");
        assertEquals("outer(inner)", text("<Template name=\"Outer\" />"));
    }

    @Test
    void wrapperForwardsItsOwnArgumentToTheInnerTemplate() {
        harness.defineTemplate("Inner", "[<Param name=\"v\" />]");
        harness
            .defineTemplate("Outer", "<Template name=\"Inner\"><Arg name=\"v\"><Param name=\"v\" /></Arg></Template>");
        assertEquals("[x]", text("<Template name=\"Outer\"><Arg name=\"v\">x</Arg></Template>"));
    }

    @Test
    void threeLevelNestingResolvesInnermost() {
        harness.defineTemplate("L3", "leaf");
        harness.defineTemplate("L2", "<Template name=\"L3\" />");
        harness.defineTemplate("L1", "<Template name=\"L2\" />");
        assertEquals("leaf", text("<Template name=\"L1\" />"));
    }

    @Test
    void deepNestingStopsAtTheDepthLimitWithoutHanging() {
        for (int level = 1; level <= 40; level++) {
            harness.defineTemplate("D" + level, level == 40 ? "bottom" : "<Template name=\"D" + (level + 1) + "\" />");
        }
        ParsedGuidePage page = harness.parse("<Template name=\"D1\" />");
        assertFalse(page.hasParseFailure(), () -> String.valueOf(page.getParseFailureMessage()));
    }

    @Test
    void selfInclusionDoesNotHangOrFailThePage() {
        harness.defineTemplate("Loop", "<Template name=\"Loop\" />");
        ParsedGuidePage page = harness.parse("<Template name=\"Loop\" />");
        assertFalse(page.hasParseFailure(), () -> String.valueOf(page.getParseFailureMessage()));
    }

    @Test
    void mutualInclusionDoesNotHang() {
        harness.defineTemplate("A", "<Template name=\"B\" />");
        harness.defineTemplate("B", "<Template name=\"A\" />");
        ParsedGuidePage page = harness.parse("<Template name=\"A\" />");
        assertFalse(page.hasParseFailure(), () -> String.valueOf(page.getParseFailureMessage()));
    }

    @Test
    void unknownTemplateReportsAnErrorButLeavesThePageCompilable() {
        String text = text("<Template name=\"Missing\" />");
        assertTrue(text.contains("Unknown template"), () -> text);
    }

    @Test
    void ifExistTakesTheBranchForAKnownTemplatePage() {
        harness.defineTemplate("Known", "present");
        harness.defineTemplate("T", "<IfExist page=\"guidenh/_en_us/templates/Known.md\">yes<Else />no</IfExist>");
        assertEquals("yes", text("<Template name=\"T\" />"));
    }

    @Test
    void ifExistTakesTheElseBranchForAnUnknownPage() {
        harness.defineTemplate("T", "<IfExist page=\"guidenh/nope.md\">yes<Else />no</IfExist>");
        assertEquals("no", text("<Template name=\"T\" />"));
    }

    @Test
    void templateBodyInFencedCodeIsNotExpanded() {
        harness.defineTemplate("T", "expanded");
        String text = text("```md\n<Template name=\"T\" />\n```\n");
        assertFalse(text.contains("expanded"), () -> "code fence must stay literal, got: " + text);
    }

    @Test
    void templateBodyInInlineCodeIsNotExpanded() {
        harness.defineTemplate("T", "expanded");
        String text = text("use `<Template name=\"T\" />` here\n");
        assertFalse(text.contains("expanded"), () -> "inline code must stay literal, got: " + text);
    }

    @Test
    void templateEmittingAnMdxTagCompiles() {
        harness.defineTemplate("T", "<Color color=\"#ff0000\">alert</Color>");
        assertTrue(text("<Template name=\"T\" />").contains("alert"), () -> text("<Template name=\"T\" />"));
    }

    @Test
    void templateEmittingAMarkdownHeadingCompiles() {
        harness.defineTemplate("T", "## Section\n\nbody");
        assertTrue(text("<Template name=\"T\" />").contains("Section"));
    }

    @Test
    void indentedCallSiteKeepsBothTemplateLines() {
        harness.defineTemplate("T", "alpha\n\nbeta");
        String text = text("- wrapper\n\n  <Template name=\"T\" />\n");
        assertTrue(text.contains("alpha"), () -> text);
        assertTrue(text.contains("beta"), () -> text);
    }

    @Test
    void templateInsideDetailsCompiles() {
        harness.defineTemplate("T", "hidden body");
        // The details block only exposes its body to a text walk when it is open, so the fixture opens it.
        String text = text("<details open>\n<summary>More</summary>\n\n<Template name=\"T\" />\n\n</details>\n");
        assertTrue(text.contains("More"), () -> text);
        assertTrue(text.contains("hidden body"), () -> text);
    }

    @Test
    void detailsInsideTemplateRendersLikeADirectDetails() {
        String body = "<details open>\n<summary>Sum</summary>\n\ninner text\n\n</details>\n";
        harness.defineTemplate("Fold", body);
        String direct = text(body);
        String throughTemplate = text("<Template name=\"Fold\" />");
        // A transcluded body has no source of its own, so the summary would be left as literal markup if the
        // compiler could not read it back out of the parsed text.
        assertEquals(direct, throughTemplate);
        assertTrue(throughTemplate.contains("Sum"), () -> throughTemplate);
        assertFalse(throughTemplate.contains("<summary>"), () -> throughTemplate);
    }

    @Test
    void rowInsideTemplateKeepsExpandedParameters() {
        // Row reads its body through the shared children path, which must see substituted values and not the
        // template's original text.
        harness.defineTemplate(
            "Line",
            "<Row gap=\"4\">\n<ItemImage id={<Param pos=\"1\" />} /> <Param pos=\"2\" />\n</Row>");
        String text = text("<Template name=\"Line\"><Arg>minecraft:iron_ingot</Arg><Arg>Iron Ingot</Arg></Template>");
        assertTrue(text.contains("Iron Ingot"), () -> text);
        assertFalse(text.contains("<Param"), () -> text);
    }

    @Test
    void resolvesAPositionalParameterUsedAsAnAttributeValue() {
        // The value can only be checked through a tag that renders a string, because an item icon needs the
        // game registry. The attribute path is the same one, so a Color id proves the substitution happened.
        harness.defineTemplate("Swatch", "<Color id={<Param pos=\"1\" />}>x</Color>");
        String text = text("<Template name=\"Swatch\"><Arg>RED</Arg></Template>");
        assertFalse(text.contains("<Param"), () -> text);
        assertFalse(
            text.contains("Expected string"),
            () -> "the attribute must be substituted before the tag reads it: " + text);
    }

    @Test
    void resolvesANamedParameterUsedAsAnAttributeValue() {
        harness.defineTemplate("Swatch", "<Color id={<Param name=\"colour\" />}>x</Color>");
        String text = text("<Template name=\"Swatch\"><Arg name=\"colour\">RED</Arg></Template>");
        assertFalse(text.contains("<Param"), () -> text);
        assertFalse(
            text.contains("Expected string"),
            () -> "the attribute must be substituted before the tag reads it: " + text);
    }

    @Test
    void twelveTemplatesOnOnePageAllExpand() {
        harness.defineTemplate("T", "c<Param pos=\"1\" />");
        StringBuilder source = new StringBuilder();
        for (int index = 0; index < 12; index++) {
            source.append("- item <Template name=\"T\"><Arg>")
                .append(index)
                .append("</Arg></Template>\n");
        }
        String rendered = text(source.toString());
        for (int index = 0; index < 12; index++) {
            int expected = index;
            assertTrue(rendered.contains("c" + expected), () -> "missing c" + expected + " in " + rendered);
        }
    }

    @Test
    void mixedTemplatesAcrossMarkdownContextsAllExpand() {
        harness.defineTemplate("H", "heading body");
        harness.defineTemplate("P", "paragraph body");
        harness.defineTemplate("L", "list body");
        // H2 rather than H1: the compiler lifts the first H1 out of the document as the page title.
        String text = text("## <Template name=\"H\" />\n\n<Template name=\"P\" />\n\n- <Template name=\"L\" />\n");
        assertTrue(text.contains("heading body"), () -> text);
        assertTrue(text.contains("paragraph body"), () -> text);
        assertTrue(text.contains("list body"), () -> text);
    }

    @Test
    void aMalformedTemplateLeavesTheRestOfThePageIntact() {
        // A parameter declaration with neither a name nor a position is malformed, which drives the error
        // path without depending on an internal failure.
        harness.defineTemplate("Broken", "before <Param /> after");
        String text = text("kept\n\n<Template name=\"Broken\" />\n\nstill kept\n");
        assertTrue(text.contains("kept"), () -> text);
        assertTrue(text.contains("still kept"), () -> text);
    }

    @Test
    void aTemplateWithNoOutputDoesNotDisturbThePage() {
        harness.defineTemplate("Empty", "");
        String text = text("kept\n\n<Template name=\"Empty\" />\n\nstill kept\n");
        assertTrue(text.contains("kept"), () -> text);
        assertTrue(text.contains("still kept"), () -> text);
    }

    private String text(String source) {
        return MediaWikiTemplateTestHarness.text(harness.compile(source));
    }
}
