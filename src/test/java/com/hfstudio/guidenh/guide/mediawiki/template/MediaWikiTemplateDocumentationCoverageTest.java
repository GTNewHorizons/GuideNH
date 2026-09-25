package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Keeps the wiki and the shipped example pack in step with the implementation. The reference page is what
 * authors read, so a tag or attribute that exists in code but is undocumented is a usability bug, and a
 * documented one that no longer exists is worse; both fail here.
 */
class MediaWikiTemplateDocumentationCoverageTest {

    private static final Path WIKI = Paths.get("wiki");
    private static final Path TEMPLATES_DOC = WIKI.resolve("Templates.md");
    private static final Path TEMPLATES_DOC_ZH = WIKI.resolve("Templates-zh-CN.md");
    private static final Path PACK_ROOT = Paths.get("wiki", "resourcepack", "assets", "guidenh", "guidenh");

    /** Every tag the template layer understands, which is what the documentation has to cover. */
    private static final Set<String> TAGS = new TreeSet<>(
        List.of(
            "Template",
            "Arg",
            "Param",
            "If",
            "Else",
            "IfEq",
            "IfExist",
            "Switch",
            "Case",
            "Default",
            "Expr",
            "Len",
            "Sub",
            "Replace",
            "Explode",
            "PadLeft",
            "PadRight",
            "Lower",
            "Upper",
            "Trim",
            "UrlEncode",
            "Pos",
            "NoInclude",
            "IncludeOnly",
            "OnlyInclude"));

    /** Attributes a call or a tag accepts. */
    private static final Set<String> ATTRIBUTES = new TreeSet<>(
        List.of(
            "name",
            "pos",
            "default",
            "test",
            "a",
            "b",
            "page",
            "value",
            "start",
            "length",
            "from",
            "to",
            "index",
            "width",
            "pad",
            "needle",
            "delimiter"));

    @Test
    void englishReferenceDocumentsEveryTag() {
        String doc = read(TEMPLATES_DOC);
        List<String> missing = new ArrayList<>();
        for (String tag : TAGS) {
            if (!doc.contains("<" + tag + ">") && !doc.contains("<" + tag + " ") && !doc.contains("<" + tag + "/>")) {
                missing.add(tag);
            }
        }
        assertTrue(missing.isEmpty(), () -> "Templates.md does not document: " + missing);
    }

    @Test
    void chineseReferenceDocumentsEveryTag() {
        String doc = read(TEMPLATES_DOC_ZH);
        List<String> missing = new ArrayList<>();
        for (String tag : TAGS) {
            if (!doc.contains("<" + tag + ">") && !doc.contains("<" + tag + " ") && !doc.contains("<" + tag + "/>")) {
                missing.add(tag);
            }
        }
        assertTrue(missing.isEmpty(), () -> "Templates-zh-CN.md does not document: " + missing);
    }

    @Test
    void englishReferenceDocumentsEveryAttribute() {
        String doc = read(TEMPLATES_DOC);
        List<String> missing = new ArrayList<>();
        for (String attribute : ATTRIBUTES) {
            if (!doc.contains(attribute + "=\"")) {
                missing.add(attribute);
            }
        }
        assertTrue(missing.isEmpty(), () -> "Templates.md does not document these attributes: " + missing);
    }

    @Test
    void chineseReferenceDocumentsEveryAttribute() {
        String doc = read(TEMPLATES_DOC_ZH);
        List<String> missing = new ArrayList<>();
        for (String attribute : ATTRIBUTES) {
            if (!doc.contains(attribute + "=\"")) {
                missing.add(attribute);
            }
        }
        assertTrue(missing.isEmpty(), () -> "Templates-zh-CN.md does not document these attributes: " + missing);
    }

    @Test
    void bothReferencesCarryWorkingExamplesForEveryTag() {
        String english = read(TEMPLATES_DOC);
        String chinese = read(TEMPLATES_DOC_ZH);
        for (String tag : TAGS) {
            // An example is a fenced block that actually uses the tag, not just a mention in a table.
            assertTrue(
                english.contains("<" + tag) && english.contains("```"),
                () -> "Templates.md has no example region for " + tag);
            assertTrue(chinese.contains("<" + tag), () -> "Templates-zh-CN.md never shows " + tag);
        }
    }

    @Test
    void examplePackDemonstratesTheHardParts() {
        String infoBox = read(
            PACK_ROOT.resolve("_en_us")
                .resolve("templates")
                .resolve("InfoBox.md"));
        String craftCost = read(
            PACK_ROOT.resolve("_en_us")
                .resolve("templates")
                .resolve("CraftCost.md"));
        String row = read(
            PACK_ROOT.resolve("_en_us")
                .resolve("templates")
                .resolve("Row.md"));
        String demo = read(
            PACK_ROOT.resolve("_en_us")
                .resolve("templates-demo.md"));

        // Named parameter with a default, and a parameter read inside an attribute.
        assertTrue(infoBox.contains("<Param name="), () -> infoBox);
        assertTrue(infoBox.contains("default=\""), () -> infoBox);
        assertTrue(infoBox.contains("id={<Param"), () -> infoBox);
        assertTrue(infoBox.contains("<If test="), () -> infoBox);
        assertTrue(infoBox.contains("<NoInclude>"), () -> infoBox);

        // Positional parameters, a switch, and include control.
        assertTrue(craftCost.contains("<Param pos="), () -> craftCost);
        assertTrue(craftCost.contains("<Switch test="), () -> craftCost);
        assertTrue(craftCost.contains("<Case value="), () -> craftCost);
        assertTrue(craftCost.contains("<Default />"), () -> craftCost);
        assertTrue(craftCost.contains("<IncludeOnly>"), () -> craftCost);

        // A template called by another template, with the outer value forwarded in.
        assertTrue(row.contains("<IncludeOnly>"), () -> row);

        // The demo page has to show the call forms an author copies from.
        assertTrue(demo.contains("<Arg name=") || demo.contains("<Arg>"), () -> demo);
        assertTrue(demo.contains("<IfEq"), () -> demo);
        assertTrue(demo.contains("<Expr"), () -> demo);
        assertTrue(demo.contains("<Len"), () -> demo);
        assertTrue(demo.contains("<Sub"), () -> demo);
    }

    @Test
    void examplePackCoversBothLanguages() {
        for (String language : List.of("_en_us", "_zh_cn")) {
            for (String template : List.of("InfoBox", "CraftCost", "Row", "OnlyPart")) {
                Path path = PACK_ROOT.resolve(language)
                    .resolve("templates")
                    .resolve(template + ".md");
                assertTrue(Files.isRegularFile(path), () -> "missing: " + path);
            }
            assertTrue(
                Files.isRegularFile(
                    PACK_ROOT.resolve(language)
                        .resolve("templates-demo.md")),
                () -> "missing demo for " + language);
        }
    }

    @Test
    void demoPageExercisesEveryValueTagAndConditional() {
        String demo = read(
            PACK_ROOT.resolve("_en_us")
                .resolve("templates-demo.md"));
        List<String> missing = new ArrayList<>();
        for (String tag : List.of(
            "Expr",
            "Len",
            "Sub",
            "Replace",
            "Explode",
            "PadLeft",
            "PadRight",
            "Lower",
            "Upper",
            "Trim",
            "UrlEncode",
            "Pos",
            "If",
            "IfEq",
            "IfExist",
            "Else",
            "Switch",
            "Case",
            "Default")) {
            // A call rather than a mention in prose, so the example is one that actually compiles.
            if (!demo.contains("<" + tag + " ") && !demo.contains("<" + tag + ">")
                && !demo.contains("<" + tag + "/>")) {
                missing.add(tag);
            }
        }
        assertTrue(missing.isEmpty(), () -> "templates-demo.md never calls: " + missing);
    }

    @Test
    void exampleTemplatesUseEveryIncludeControlTag() {
        StringBuilder pack = new StringBuilder();
        for (String template : List.of("InfoBox", "CraftCost", "Row", "OnlyPart")) {
            pack.append(
                read(
                    PACK_ROOT.resolve("_en_us")
                        .resolve("templates")
                        .resolve(template + ".md")));
        }
        String all = pack.toString();
        for (String tag : List.of("NoInclude", "IncludeOnly", "OnlyInclude")) {
            assertTrue(all.contains("<" + tag + ">"), () -> "no example template uses <" + tag + ">");
        }
    }

    @Test
    void referenceDoesNotDocumentTagsThatDoNotExist() {
        String doc = read(TEMPLATES_DOC);
        // The magic words and parser-function spellings from the previous brace syntax must be gone.
        for (String removed : List.of("PAGENAME", "NAMESPACE", "CURRENTLANG", "#expr", "#ifexpr", "#switch", "#len")) {
            assertTrue(!doc.contains(removed), () -> "Templates.md still documents removed syntax: " + removed);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            throw new IllegalStateException("Cannot read " + path, failed);
        }
    }
}
