package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

class MediaWikiTemplatePageIdsTest {

    @Test
    void detectsTemplatePagesAtAnyDepth() {
        assertTrue(MediaWikiTemplatePageIds.isTemplatePage(id("guidenh/guidenh/_en_us/templates/InfoBox.md")));
        assertTrue(MediaWikiTemplatePageIds.isTemplatePage(id("guidenh/guidenh/_zh_cn/templates/nav/Row.md")));
    }

    @Test
    void rejectsOrdinaryPages() {
        assertFalse(MediaWikiTemplatePageIds.isTemplatePage(id("guidenh/guidenh/_en_us/index.md")));
        assertFalse(MediaWikiTemplatePageIds.isTemplatePage(id("guidenh/guidenh/_en_us/templated.md")));
        assertFalse(MediaWikiTemplatePageIds.isTemplatePage(null));
    }

    @Test
    void extractsNestedTemplateName() {
        assertEquals(
            "InfoBox",
            MediaWikiTemplatePageIds.templateNameOf(id("guidenh/guidenh/_en_us/templates/InfoBox.md")));
        assertEquals(
            "nav/Row",
            MediaWikiTemplatePageIds.templateNameOf(id("guidenh/guidenh/_zh_cn/templates/nav/Row.md")));
        assertNull(MediaWikiTemplatePageIds.templateNameOf(id("guidenh/guidenh/_en_us/index.md")));
    }

    @Test
    void buildsPageIdForTemplate() {
        ResourceLocation built = MediaWikiTemplatePageIds.pageIdFor("guidenh", "guidenh", "_en_us", "InfoBox");
        assertEquals(new ResourceLocation("guidenh", "guidenh/_en_us/templates/InfoBox.md"), built);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("guidenh", path);
    }
}
