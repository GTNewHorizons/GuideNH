package com.hfstudio.guidenh.guide.internal.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVBox;

/**
 * Structural tests for the settings document builder. Builds
 * documents headlessly (the builder never touches GL / Minecraft singletons)
 * and walks the tree with {@link LytNode#getTextContent()}.
 */
class GuideSettingsDocumentBuilderTest {

    private static final int[] PRESETS = GuideSettingsDocumentBuilder.FONT_SIZE_PRESETS;
    private static final String[] BODY_FAMILY_PRESETS = GuideSettingsDocumentBuilder.BODY_FONT_FAMILY_PRESETS;
    private static final String[] CODE_FAMILY_PRESETS = GuideSettingsDocumentBuilder.CODE_FONT_FAMILY_PRESETS;

    @AfterEach
    void restoreReaderConfig() {
        ModConfig.reader.readerFontSize = 0;
        ModConfig.reader.readerFontFamily = "system";
        ModConfig.reader.readerCodeFontFamily = "mono";
    }

    @Test
    void buildDocumentContainsReaderCategoryItem() {
        var document = GuideSettingsDocumentBuilder.buildDocument();
        String text = document.getTextContent();
        assertTrue(text.contains("\u9605\u8BFB"), "sidebar must contain the Reading category label");
        assertTrue(text.contains("Reader"), "sidebar must contain the Reader category");
    }

    @Test
    void buildDocumentContainsFontSizeRowLabel() {
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertTrue(
            document.getTextContent()
                .contains("\u5B57\u53F7 / Font size"),
            "font size row must carry the bilingual label");
    }

    @Test
    void valueEchoesConfiguredReaderFontSize() {
        ModConfig.reader.readerFontSize = 13;
        var document = GuideSettingsDocumentBuilder.buildDocument();
        String text = document.getTextContent();
        assertTrue(text.contains("13"), "current value must echo the configured size");
        assertFalse(text.contains("\u8DDF\u968F\u7CFB\u7EDF"), "non-zero size must not render the follow-system label");
    }

    @Test
    void valueShowsFollowSystemWhenZero() {
        ModConfig.reader.readerFontSize = 0;
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertTrue(
            document.getTextContent()
                .contains("\u8DDF\u968F\u7CFB\u7EDF"),
            "zero must render the follow-system label");
    }

    @Test
    void presetsAreStrictlyAscendingAndStartAtFollowSystem() {
        assertEquals(0, PRESETS[0], "first preset must be 0 (follow system)");
        for (int i = 1; i < PRESETS.length; i++) {
            assertTrue(PRESETS[i] > PRESETS[i - 1], "presets must be strictly ascending at index " + i);
        }
    }

    @Test
    void nextWalksAllPresetsAndClampsAtTop() {
        for (int i = 1; i < PRESETS.length; i++) {
            assertEquals(
                PRESETS[i],
                GuideSettingsDocumentBuilder.FontSizePresets.next(PRESETS[i - 1]),
                "next must step onto the following preset from " + PRESETS[i - 1]);
        }
        assertEquals(20, GuideSettingsDocumentBuilder.FontSizePresets.next(20), "top endpoint must clamp");
        assertEquals(20, GuideSettingsDocumentBuilder.FontSizePresets.next(21), "above-range must clamp at max");
    }

    @Test
    void prevWalksAllPresetsAndClampsAtBottom() {
        for (int i = 1; i < PRESETS.length; i++) {
            assertEquals(
                PRESETS[i - 1],
                GuideSettingsDocumentBuilder.FontSizePresets.prev(PRESETS[i]),
                "prev must step onto the preceding preset from " + PRESETS[i]);
        }
        assertEquals(0, GuideSettingsDocumentBuilder.FontSizePresets.prev(0), "bottom endpoint must clamp");
        assertEquals(0, GuideSettingsDocumentBuilder.FontSizePresets.prev(-1), "below-range must clamp at min");
    }

    @Test
    void buildDocumentContainsPlusMinusStepperButtons() {
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertNotNull(findTextButton(document, "\u2212"), "document must contain the minus step button");
        assertNotNull(findTextButton(document, "+"), "document must contain the plus step button");
    }

    @Test
    void applyFontSizeStepPersistsConfigValue() {
        ModConfig.reader.readerFontSize = 12;
        GuideSettingsDocumentBuilder.applyFontSizeStep(true);
        assertEquals(13, ModConfig.reader.readerFontSize, "increase must move to the next preset");

        GuideSettingsDocumentBuilder.applyFontSizeStep(false);
        assertEquals(12, ModConfig.reader.readerFontSize, "decrease must move back to the previous preset");
    }

    @Test
    void applyFontSizeStepClampsAtPresetEndpoints() {
        ModConfig.reader.readerFontSize = 20;
        GuideSettingsDocumentBuilder.applyFontSizeStep(true);
        assertEquals(20, ModConfig.reader.readerFontSize, "increase at max preset must not change the value");

        ModConfig.reader.readerFontSize = 0;
        GuideSettingsDocumentBuilder.applyFontSizeStep(false);
        assertEquals(0, ModConfig.reader.readerFontSize, "decrease at follow-system preset must not change the value");
    }

    @Test
    void rowsAppearInDeclaredOrder() {
        var document = GuideSettingsDocumentBuilder.buildDocument();
        var rowsVBox = (LytVBox) document.getChildren()
            .get(0)
            .getChildren()
            .get(1);
        var rows = rowsVBox.getChildren();
        assertEquals(3, rows.size(), "settings page must carry exactly three rows");
        assertTrue(
            rows.get(0)
                .getTextContent()
                .contains("\u5B57\u53F7 / Font size"),
            "row 1 must be the font-size row");
        assertTrue(
            rows.get(1)
                .getTextContent()
                .contains("\u6B63\u6587\u5B57\u4F53 / Font family"),
            "row 2 must be the body font-family row");
        assertTrue(
            rows.get(2)
                .getTextContent()
                .contains("\u4EE3\u7801\u5B57\u4F53 / Code font"),
            "row 3 must be the code font-family row");
    }

    @Test
    void settingsPageCarriesSixStepperButtons() {
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertEquals(6, countTextButtons(document), "three rows x 2 stepper buttons must yield 6 TextButtons");
    }

    @Test
    void familyPresetsMatchDeclaredSpec() {
        assertArrayEquals(
            new String[] { "system", "serif", "monospace" },
            BODY_FAMILY_PRESETS,
            "body font-family presets must match the executor-fixed stepping order");
        assertArrayEquals(
            new String[] { "mono", "Consolas", "Courier New" },
            CODE_FAMILY_PRESETS,
            "code font-family presets must match the executor-fixed stepping order");
    }

    @Test
    void bodyFamilyNextWalksAllPresetsAndClampsAtTop() {
        for (int i = 1; i < BODY_FAMILY_PRESETS.length; i++) {
            assertEquals(
                BODY_FAMILY_PRESETS[i],
                GuideSettingsDocumentBuilder.StringPresets.next(BODY_FAMILY_PRESETS, BODY_FAMILY_PRESETS[i - 1]),
                "next must step onto the following body-family preset from " + BODY_FAMILY_PRESETS[i - 1]);
        }
        String top = BODY_FAMILY_PRESETS[BODY_FAMILY_PRESETS.length - 1];
        assertEquals(
            top,
            GuideSettingsDocumentBuilder.StringPresets.next(BODY_FAMILY_PRESETS, top),
            "body-family top endpoint must clamp");
        assertEquals(
            top,
            GuideSettingsDocumentBuilder.StringPresets.next(BODY_FAMILY_PRESETS, "unknown"),
            "body-family out-of-preset value must clamp at max");
    }

    @Test
    void bodyFamilyPrevWalksAllPresetsAndClampsAtBottom() {
        for (int i = 1; i < BODY_FAMILY_PRESETS.length; i++) {
            assertEquals(
                BODY_FAMILY_PRESETS[i - 1],
                GuideSettingsDocumentBuilder.StringPresets.prev(BODY_FAMILY_PRESETS, BODY_FAMILY_PRESETS[i]),
                "prev must step onto the preceding body-family preset from " + BODY_FAMILY_PRESETS[i]);
        }
        String bottom = BODY_FAMILY_PRESETS[0];
        assertEquals(
            bottom,
            GuideSettingsDocumentBuilder.StringPresets.prev(BODY_FAMILY_PRESETS, bottom),
            "body-family bottom endpoint must clamp");
        assertEquals(
            bottom,
            GuideSettingsDocumentBuilder.StringPresets.prev(BODY_FAMILY_PRESETS, "unknown"),
            "body-family out-of-preset value must clamp at min");
    }

    @Test
    void codeFamilyNextWalksAllPresetsAndClampsAtTop() {
        for (int i = 1; i < CODE_FAMILY_PRESETS.length; i++) {
            assertEquals(
                CODE_FAMILY_PRESETS[i],
                GuideSettingsDocumentBuilder.StringPresets.next(CODE_FAMILY_PRESETS, CODE_FAMILY_PRESETS[i - 1]),
                "next must step onto the following code-family preset from " + CODE_FAMILY_PRESETS[i - 1]);
        }
        String top = CODE_FAMILY_PRESETS[CODE_FAMILY_PRESETS.length - 1];
        assertEquals(
            top,
            GuideSettingsDocumentBuilder.StringPresets.next(CODE_FAMILY_PRESETS, top),
            "code-family top endpoint must clamp");
    }

    @Test
    void codeFamilyPrevWalksAllPresetsAndClampsAtBottom() {
        for (int i = 1; i < CODE_FAMILY_PRESETS.length; i++) {
            assertEquals(
                CODE_FAMILY_PRESETS[i - 1],
                GuideSettingsDocumentBuilder.StringPresets.prev(CODE_FAMILY_PRESETS, CODE_FAMILY_PRESETS[i]),
                "prev must step onto the preceding code-family preset from " + CODE_FAMILY_PRESETS[i]);
        }
        String bottom = CODE_FAMILY_PRESETS[0];
        assertEquals(
            bottom,
            GuideSettingsDocumentBuilder.StringPresets.prev(CODE_FAMILY_PRESETS, bottom),
            "code-family bottom endpoint must clamp");
    }

    @Test
    void valueEchoesConfiguredBodyFontFamily() {
        ModConfig.reader.readerFontFamily = "serif";
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertTrue(
            document.getTextContent()
                .contains("serif"),
            "body font-family value must echo the configured raw string");
    }

    @Test
    void valueEchoesConfiguredCodeFontFamily() {
        ModConfig.reader.readerCodeFontFamily = "Consolas";
        var document = GuideSettingsDocumentBuilder.buildDocument();
        assertTrue(
            document.getTextContent()
                .contains("Consolas"),
            "code font-family value must echo the configured raw string");
    }

    @Test
    void applyBodyFontFamilyStepPersistsConfigValue() {
        ModConfig.reader.readerFontFamily = "system";
        GuideSettingsDocumentBuilder.applyBodyFontFamilyStep(true);
        assertEquals("serif", ModConfig.reader.readerFontFamily, "increase must move to the next body-family preset");

        GuideSettingsDocumentBuilder.applyBodyFontFamilyStep(false);
        assertEquals(
            "system",
            ModConfig.reader.readerFontFamily,
            "decrease must move back to the previous body-family preset");
    }

    @Test
    void applyBodyFontFamilyStepClampsAtPresetEndpoints() {
        ModConfig.reader.readerFontFamily = "monospace";
        GuideSettingsDocumentBuilder.applyBodyFontFamilyStep(true);
        assertEquals(
            "monospace",
            ModConfig.reader.readerFontFamily,
            "increase at the top preset must not change the value");

        ModConfig.reader.readerFontFamily = "system";
        GuideSettingsDocumentBuilder.applyBodyFontFamilyStep(false);
        assertEquals(
            "system",
            ModConfig.reader.readerFontFamily,
            "decrease at the bottom preset must not change the value");
    }

    @Test
    void applyCodeFontFamilyStepPersistsConfigValue() {
        ModConfig.reader.readerCodeFontFamily = "mono";
        GuideSettingsDocumentBuilder.applyCodeFontFamilyStep(true);
        assertEquals(
            "Consolas",
            ModConfig.reader.readerCodeFontFamily,
            "increase must move to the next code-family preset");

        GuideSettingsDocumentBuilder.applyCodeFontFamilyStep(false);
        assertEquals(
            "mono",
            ModConfig.reader.readerCodeFontFamily,
            "decrease must move back to the previous code-family preset");
    }

    @Test
    void applyCodeFontFamilyStepClampsAtPresetEndpoints() {
        ModConfig.reader.readerCodeFontFamily = "Courier New";
        GuideSettingsDocumentBuilder.applyCodeFontFamilyStep(true);
        assertEquals(
            "Courier New",
            ModConfig.reader.readerCodeFontFamily,
            "increase at the top preset must not change the value");

        ModConfig.reader.readerCodeFontFamily = "mono";
        GuideSettingsDocumentBuilder.applyCodeFontFamilyStep(false);
        assertEquals(
            "mono",
            ModConfig.reader.readerCodeFontFamily,
            "decrease at the bottom preset must not change the value");
    }

    private static int countTextButtons(LytNode node) {
        int count = node instanceof GuideSettingsDocumentBuilder.TextButton ? 1 : 0;
        for (var child : node.getChildren()) {
            count += countTextButtons(child);
        }
        return count;
    }

    private static GuideSettingsDocumentBuilder.TextButton findTextButton(LytNode node, String label) {
        if (node instanceof GuideSettingsDocumentBuilder.TextButton button && label.equals(button.getLabel())) {
            return button;
        }
        for (var child : node.getChildren()) {
            GuideSettingsDocumentBuilder.TextButton found = findTextButton(child, label);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
