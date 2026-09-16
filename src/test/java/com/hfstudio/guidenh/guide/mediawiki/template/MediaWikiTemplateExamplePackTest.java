package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Compiles the templates shipped in the documentation's example pack, read from disk. The pack is what the
 * wiki tells authors to write, so compiling it keeps the documented syntax and the implementation together:
 * a change that breaks the documented form fails here rather than only in game.
 */
class MediaWikiTemplateExamplePackTest {

    private static final Path PACK_ROOT = Paths.get("wiki", "resourcepack", "assets", "guidenh", "guidenh");
    private static final List<String> LANGUAGES = List.of("_en_us", "_zh_cn");
    private static final List<String> TEMPLATES = List.of("InfoBox", "CraftCost", "Row", "OnlyPart");

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @AfterEach
    void clear() {
        MediaWikiTemplateRepository.clear();
        MediaWikiTemplateDependencyGraph.clear();
    }

    @Test
    void everyExampleTemplatePageExists() {
        for (String language : LANGUAGES) {
            for (String template : TEMPLATES) {
                Path path = templatePath(language, template);
                assertTrue(Files.isRegularFile(path), () -> "missing example template: " + path);
            }
        }
    }

    @Test
    void exampleTemplatesParse() {
        for (String language : LANGUAGES) {
            for (String template : TEMPLATES) {
                var page = harness.parse(read(templatePath(language, template)));
                assertFalse(
                    page.hasParseFailure(),
                    () -> language + "/" + template + " failed to parse: " + page.getParseFailureMessage());
            }
        }
    }

    @Test
    void infoBoxRendersArgumentsWrittenOnTheirOwnLines() {
        register("_en_us", "InfoBox");
        // Writing each <Arg> on its own line makes the parser wrap them in a paragraph, which is the form the
        // documentation shows and the form that once dropped every argument.
        String text = harness.textOfPage(
            "<Template name=\"InfoBox\">\n  <Arg name=\"name\">Steel Ingot</Arg>\n"
                + "  <Arg name=\"icon\">minecraft:iron_ingot</Arg>\n</Template>");
        assertTrue(text.contains("Steel Ingot"), () -> text);
        assertFalse(text.contains("Untitled"), () -> "an argument written on its own line must still apply: " + text);
    }

    @Test
    void craftCostResolvesPositionalArgumentsWrittenOnTheirOwnLines() {
        registerCraftCost();
        String text = harness.textOfPage(
            "<Template name=\"CraftCost\" tier=\"mv\">\n  <Arg>minecraft:iron_ingot</Arg>\n"
                + "  <Arg>Iron Ingot</Arg>\n  <Arg>2 iron ingots</Arg>\n</Template>");
        assertTrue(text.contains("Iron Ingot"), () -> text);
        assertTrue(text.contains("2 iron ingots"), () -> text);
        assertFalse(text.contains("Unknown"), () -> "the positional defaults must not appear: " + text);
    }

    @Test
    void infoBoxRendersItsArguments() {
        register("_en_us", "InfoBox");
        String text = harness.textOfPage(
            "<Template name=\"InfoBox\"><Arg name=\"name\">Steel Ingot</Arg>"
                + "<Arg name=\"icon\">minecraft:iron_ingot</Arg></Template>");
        assertTrue(text.contains("Steel Ingot"), () -> text);
        assertFalse(text.contains("Untitled"), () -> "the default must not appear once an argument is given: " + text);
    }

    @Test
    void infoBoxFallsBackToItsDefaults() {
        register("_en_us", "InfoBox");
        String text = harness.textOfPage("<Template name=\"InfoBox\" />");
        assertTrue(text.contains("Untitled"), () -> text);
    }

    @Test
    void infoBoxShowsNothingForAnAbsentNoteButShowsAPresentOne() {
        register("_en_us", "InfoBox");
        String withoutNote = harness.textOfPage("<Template name=\"InfoBox\"><Arg name=\"name\">Steel</Arg></Template>");
        assertFalse(withoutNote.contains("no value"), () -> withoutNote);

        String withNote = harness.textOfPage(
            "<Template name=\"InfoBox\"><Arg name=\"name\">Steel</Arg>"
                + "<Arg name=\"note\">Smelted from iron.</Arg></Template>");
        assertTrue(withNote.contains("Smelted from iron."), () -> withNote);
    }

    @Test
    void craftCostResolvesItsTierThroughSwitch() {
        registerCraftCost();
        String text = harness.textOfPage(
            "<Template name=\"CraftCost\" tier=\"mv\">"
                + "<Arg>minecraft:iron_ingot</Arg><Arg>Iron Ingot</Arg><Arg>2 iron ingots</Arg></Template>");
        assertTrue(text.contains("MV"), () -> text);
        assertTrue(text.contains("Iron Ingot"), () -> text);
    }

    @Test
    void craftCostPrintsNothingForAnUnknownTier() {
        registerCraftCost();
        String unknown = harness.textOfPage(
            "<Template name=\"CraftCost\" tier=\"zzz\">"
                + "<Arg>minecraft:iron_ingot</Arg><Arg>Iron Ingot</Arg><Arg>2 iron ingots</Arg></Template>");
        assertFalse(unknown.contains("Voltage tier"), () -> unknown);
        assertTrue(unknown.contains("Iron Ingot"), () -> unknown);
    }

    @Test
    void craftCostIsTranscludedWithoutItsDocumentation() {
        registerCraftCost();
        String text = harness.textOfPage(
            "<Template name=\"CraftCost\" tier=\"mv\">"
                + "<Arg>minecraft:iron_ingot</Arg><Arg>Iron Ingot</Arg><Arg>2 iron ingots</Arg></Template>");
        assertFalse(text.contains("Usage"), () -> "NoInclude documentation must not be transcluded: " + text);
    }

    @Test
    void rowIsIncludedThroughCraftCostWithoutLeakingItsDefaults() {
        registerCraftCost();
        String text = harness.textOfPage(
            "<Template name=\"CraftCost\" tier=\"lv\">"
                + "<Arg>minecraft:redstone</Arg><Arg>Redstone</Arg><Arg>4 redstone dust</Arg></Template>");
        assertTrue(text.contains("4 redstone dust"), () -> text);
        assertFalse(text.contains("minecraft:book"), () -> "the Row default must not leak: " + text);
    }

    @Test
    void chineseExamplePackCompilesToo() {
        register("_zh_cn", "InfoBox");
        String text = harness.textOfPage("<Template name=\"InfoBox\" />");
        assertTrue(text.contains("未命名"), () -> text);
    }

    @Test
    void onlyIncludeKeepsOnlyItsOwnBody() {
        register("_en_us", "OnlyPart");
        String text = harness.textOfPage("<Template name=\"OnlyPart\" />");
        assertTrue(text.contains("only part that is transcluded"), () -> text);
        assertFalse(text.contains("never\nsees it"), () -> "text outside OnlyInclude must not be transcluded: " + text);
    }

    @Test
    void demoPageCompilesEveryCallItShows() {
        register("_en_us", "InfoBox");
        register("_en_us", "CraftCost");
        register("_en_us", "Row");
        register("_en_us", "OnlyPart");
        Path demo = PACK_ROOT.resolve("_en_us")
            .resolve("templates-demo.md");
        // Compiled rather than merely parsed, so a demo call that cannot resolve shows up here.
        String text = harness.textOfPage(read(demo));
        assertTrue(text.contains("Steel Ingot"), () -> text);
    }

    @Test
    void demoPageParsesWithoutFailure() {
        for (String language : LANGUAGES) {
            Path demo = PACK_ROOT.resolve(language)
                .resolve("templates-demo.md");
            assertTrue(Files.isRegularFile(demo), () -> "missing demo page: " + demo);
            String source = read(demo);
            var page = harness.parse(source);
            assertFalse(
                page.hasParseFailure(),
                () -> language + " demo failed to parse: " + page.getParseFailureMessage());
            // The demo deliberately calls an unknown template to show the failure path.
            assertTrue(source.contains("<Template name=\"NoSuchTemplateHere\" />"), () -> language);
        }
    }

    private void registerCraftCost() {
        register("_en_us", "CraftCost");
        register("_en_us", "Row");
    }

    private void register(String language, String template) {
        harness.defineTemplate(template, read(templatePath(language, template)));
    }

    private static Path templatePath(String language, String template) {
        return PACK_ROOT.resolve(language)
            .resolve("templates")
            .resolve(template + ".md");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            throw new IllegalStateException("Cannot read " + path, failed);
        }
    }
}
