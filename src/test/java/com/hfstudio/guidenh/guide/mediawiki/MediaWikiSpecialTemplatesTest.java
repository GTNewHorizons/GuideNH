package com.hfstudio.guidenh.guide.mediawiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The template special page is registered like every other list, so the catalog is what decides whether it
 * appears in the special-page index and which group it lands in.
 */
class MediaWikiSpecialTemplatesTest {

    @Test
    void theTemplatesSpecialPageIsRegistered() {
        MediaWikiSpecialDefinition definition = MediaWikiSpecialCatalog.findByName(MediaWikiSpecialPageIds.TEMPLATES);
        assertNotNull(definition, "Templates must be a known special page");
        assertEquals("lists", definition.groupName(), "a list of template pages belongs with the other lists");
        assertEquals("guidenh.mediawiki.special.templates", definition.titleKey());
        assertEquals(MediaWikiSpecialPageKind.GRID, definition.kind());
    }

    @Test
    void everyDefinitionResolvesByName() {
        for (MediaWikiSpecialDefinition definition : MediaWikiSpecialCatalog.definitions()) {
            assertEquals(
                definition,
                MediaWikiSpecialCatalog.findByName(definition.name()),
                () -> "the catalog must resolve its own entry: " + definition.name());
        }
    }

    @Test
    void theListsGroupExists() {
        assertTrue(
            MediaWikiSpecialCatalog.groups()
                .stream()
                .anyMatch(group -> "lists".equals(group.name())),
            "the lists group must be declared");
        assertTrue(
            MediaWikiSpecialCatalog.definitions()
                .stream()
                .anyMatch(definition -> "lists".equals(definition.groupName())),
            "at least one definition must belong to the lists group");
    }
}
