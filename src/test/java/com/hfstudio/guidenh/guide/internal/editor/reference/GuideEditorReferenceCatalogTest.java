package com.hfstudio.guidenh.guide.internal.editor.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GuideEditorReferenceCatalogTest {

    @Test
    public void examplesAndCommentsCannotCreateTopics() {
        assertTrue(
            GuideEditorReferenceCatalog.splitSections("Tags-Reference", "")
                .isEmpty());
        String source = "# Tags\nIntro\n{/*\n## Hidden\n*/}\n## Recipes\n````md\n## Example\n```\n````\n"
            + "~~~~md\n### Another example\n~~~\n~~~~\n### Links\n<ItemLink id=\"minecraft:stone\" />\n";
        var sections = GuideEditorReferenceCatalog.splitSections("Tags-Reference", source);

        assertEquals(
            List.of("Tags", "Recipes", "Links"),
            sections.stream()
                .map(GuideEditorReferenceSection::title)
                .toList());
        assertTrue(
            sections.get(1)
                .markdown()
                .contains("## Example"));
    }

    @Test
    public void searchIncludesAttributesAndExamplesWithoutChangingIdCase() {
        var sections = GuideEditorReferenceCatalog.splitSections(
            "Tags-Reference",
            "# Tags\n## Research\n```mdx\n<ResearchLink id=\"INFUSION\" showTooltip=\"true\" />\n```\n");
        var catalog = new GuideEditorReferenceCatalog(sections);

        assertEquals(
            "Research",
            catalog.search(" SHOWTOOLTIP ")
                .getFirst()
                .title());
        assertEquals(
            "Research",
            catalog.search("infusion")
                .getFirst()
                .title());
        assertTrue(
            catalog.search("infusion")
                .getFirst()
                .markdown()
                .contains("INFUSION"));
        assertTrue(
            catalog.search("unknown_attribute")
                .isEmpty());
    }

    @Test
    public void linksResolveDocumentAliasesAndNestedUnicodeAnchors() {
        var sections = GuideEditorReferenceCatalog
            .splitSections("Tags-Reference", "# 标签\n## 研究绑定\n#### `showTooltip` 参数\nText\n");
        var catalog = new GuideEditorReferenceCatalog(sections);

        assertEquals(sections.get(1), catalog.find("./Tags-Reference-zh-CN.md", "showtooltip-参数"));
        assertEquals(sections.getFirst(), catalog.find("Tags-Reference", ""));
        assertNull(catalog.find("Missing", ""));
    }
}
