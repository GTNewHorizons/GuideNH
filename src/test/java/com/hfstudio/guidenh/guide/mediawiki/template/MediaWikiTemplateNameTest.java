package com.hfstudio.guidenh.guide.mediawiki.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MediaWikiTemplateNameTest {

    @Test
    void foldsFirstCharacterOnly() {
        assertEquals(MediaWikiTemplateName.parse("infobox"), MediaWikiTemplateName.parse("Infobox"));
        assertNotEquals(MediaWikiTemplateName.parse("Infobox"), MediaWikiTemplateName.parse("InfoBox"));
    }

    @Test
    void turnsUnderscoresIntoSpaces() {
        assertEquals(
            "Info box",
            MediaWikiTemplateName.parse("info_box")
                .value());
    }

    @Test
    void collapsesRepeatedSpaces() {
        assertEquals(
            "Info box",
            MediaWikiTemplateName.parse("  info   box  ")
                .value());
    }

    @Test
    void emptyInputStaysEmpty() {
        assertTrue(
            MediaWikiTemplateName.parse("")
                .isEmpty());
        assertTrue(
            MediaWikiTemplateName.parse(null)
                .isEmpty());
        assertTrue(
            MediaWikiTemplateName.parse("   ")
                .isEmpty());
    }
}
