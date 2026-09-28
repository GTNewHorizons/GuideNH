package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The dependency graph that makes an edited template reach the pages which used it. Edges are recorded while
 * compiling, so these cases drive the real compiler and then ask the graph what a template edit affects.
 */
class MediaWikiTemplateDependencyGraphTest {

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @AfterEach
    void clear() {
        MediaWikiTemplateRepository.clear();
        MediaWikiTemplateDependencyGraph.clear();
    }

    @Test
    void compilingAPageRecordsTheTemplateItUsed() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/page.md", "<Template name=\"Box\" />");

        Set<MediaWikiTemplateName> used = MediaWikiTemplateDependencyGraph.usedBy(pageId("guidenh/page.md"));
        assertEquals(
            List.of("Box"),
            used.stream()
                .map(MediaWikiTemplateName::value)
                .toList());
    }

    @Test
    void anEditedTemplateFindsThePageThatUsedIt() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/page.md", "<Template name=\"Box\" />");

        Set<ResourceLocation> dependents = MediaWikiTemplateDependencyGraph
            .dependentsOf(MediaWikiTemplateName.parse("Box"));
        assertTrue(dependents.contains(pageId("guidenh/page.md")), () -> dependents.toString());
    }

    @Test
    void aPageThatUsedNoTemplateIsNotADependent() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/other.md", "plain text");

        assertTrue(
            MediaWikiTemplateDependencyGraph.dependentsOf(MediaWikiTemplateName.parse("Box"))
                .isEmpty());
    }

    @Test
    void dependentsAreTransitiveThroughAWrapperTemplate() {
        harness.defineTemplate("Inner", "inner");
        harness.defineTemplate("Wrapper", "<Template name=\"Inner\" />");
        harness.compilePage("guidenh/page.md", "<Template name=\"Wrapper\" />");

        Set<ResourceLocation> dependents = MediaWikiTemplateDependencyGraph
            .dependentsOf(MediaWikiTemplateName.parse("Inner"));
        // The page reached Inner through Wrapper, so editing Inner has to rebuild the page as well.
        assertTrue(dependents.contains(pageId("guidenh/page.md")), () -> dependents.toString());
        assertTrue(
            dependents.contains(MediaWikiTemplateTestHarness.templatePageId("Wrapper")),
            () -> dependents.toString());
    }

    @Test
    void recompilingAPageReplacesItsOldEdges() {
        harness.defineTemplate("First", "a");
        harness.defineTemplate("Second", "b");
        harness.compilePage("guidenh/page.md", "<Template name=\"First\" />");
        harness.compilePage("guidenh/page.md", "<Template name=\"Second\" />");

        assertFalse(
            MediaWikiTemplateDependencyGraph.dependentsOf(MediaWikiTemplateName.parse("First"))
                .contains(pageId("guidenh/page.md")),
            "a stale edge would rebuild a page for a template it no longer uses");
        assertTrue(
            MediaWikiTemplateDependencyGraph.dependentsOf(MediaWikiTemplateName.parse("Second"))
                .contains(pageId("guidenh/page.md")));
    }

    @Test
    void unknownTemplateCreatesNoEdge() {
        harness.compilePage("guidenh/page.md", "<Template name=\"Missing\" />");
        assertEquals(
            0,
            MediaWikiTemplateDependencyGraph.dependentsOf(MediaWikiTemplateName.parse("Missing"))
                .size());
    }

    @Test
    void invalidatorCollectsChangedTemplatesAndTheirDependents() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/page.md", "<Template name=\"Box\" />");

        List<ResourceLocation> changed = new ArrayList<>();
        changed.add(MediaWikiTemplateTestHarness.templatePageId("Box"));
        Set<ResourceLocation> affected = MediaWikiTemplateInvalidator.collectAffected(changed);

        assertTrue(affected.contains(MediaWikiTemplateTestHarness.templatePageId("Box")));
        assertTrue(affected.contains(pageId("guidenh/page.md")), () -> affected.toString());
    }

    @Test
    void invalidatorReportsWhatItInvalidated() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/page.md", "<Template name=\"Box\" />");

        List<ResourceLocation> invalidated = new ArrayList<>();
        List<ResourceLocation> recompiled = new ArrayList<>();
        Set<ResourceLocation> affected = MediaWikiTemplateInvalidator.onTemplatePagesChanged(
            List.of(MediaWikiTemplateTestHarness.templatePageId("Box")),
            invalidated::add,
            recompiled::add);

        assertEquals(affected.size(), invalidated.size());
        assertEquals(invalidated.size(), recompiled.size());
    }

    @Test
    void graphClears() {
        harness.defineTemplate("Box", "body");
        harness.compilePage("guidenh/page.md", "<Template name=\"Box\" />");
        assertTrue(MediaWikiTemplateDependencyGraph.pageCount() > 0);

        MediaWikiTemplateDependencyGraph.clear();
        assertEquals(0, MediaWikiTemplateDependencyGraph.pageCount());
        assertEquals(0, MediaWikiTemplateDependencyGraph.edgeCount());
    }

    private static ResourceLocation pageId(String path) {
        return new ResourceLocation(MediaWikiTemplateTestHarness.PACK, path);
    }
}
