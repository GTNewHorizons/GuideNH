package com.hfstudio.guidenh.guide.internal.settings;

import java.util.function.Consumer;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytButton;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

/**
 * Builds the reader settings page document.
 * <p>
 * Pure static construction: {@link #buildDocument()} only assembles the Lyt
 * tree and reads {@link ModConfig.reader}, with no GL, font loading or Minecraft
 * singleton access, so the same code path is headless-testable. The ± click
 * callbacks (which do touch the screen to refresh the echo) only fire at
 * render-time interaction, never during build.
 * <p>
 * Rows in declared order: font size, body font family, code font
 * family; all three share {@link #buildStepperRow(String, String, String,
 * String, Consumer, Consumer)} so they are structurally identical by
 * construction.
 */
public class GuideSettingsDocumentBuilder {

    public static final ConstantColor DIVIDER_COLOR = new ConstantColor(0xFF3A3A3A);
    public static final ConstantColor CATEGORY_SELECTED_COLOR = new ConstantColor(0xFF00D2FC);

    /** Ascending presets, first entry 0 = follow system (see GuideText.resolveBaseFontSize). */
    public static final int[] FONT_SIZE_PRESETS = { 0, 9, 10, 11, 12, 13, 14, 16, 18, 20 };

    /**
     * Body font-family presets. The array order IS the
     * stepping order; the raw config string is displayed untranslated.
     */
    public static final String[] BODY_FONT_FAMILY_PRESETS = { "system", "serif", "monospace" };

    /**
     * Code font-family presets. The array order IS the
     * stepping order; the raw config string is displayed untranslated.
     */
    public static final String[] CODE_FONT_FAMILY_PRESETS = { "mono", "Consolas", "Courier New" };

    private static final int STEP_BUTTON_WIDTH = 18;
    private static final int STEP_BUTTON_HEIGHT = 16;

    private GuideSettingsDocumentBuilder() {}

    /** Builds the settings page tree: category sidebar on the left, setting rows on the right. */
    public static LytDocument buildDocument() {
        var document = new LytDocument();
        var root = new LytHBox();
        root.setFullWidth(true);
        root.setWrap(false);
        root.setGap(16);
        root.setAlignItems(AlignItems.START);
        root.append(buildCategorySidebar());
        root.append(buildSettingsRows());
        document.append(root);
        return document;
    }

    private static LytVBox buildCategorySidebar() {
        var sidebar = new LytVBox();
        sidebar.setGap(4);
        sidebar.setPaddingRight(8);
        sidebar.setFullWidth(false);

        // Single category today; the selected-state visual is bold + accent
        // color (paragraphs have no border/background rendering).
        var readerCategory = new LytParagraph();
        readerCategory.appendText("阅读 / Reader");
        readerCategory.modifyStyle(
            style -> style.bold(true)
                .color(CATEGORY_SELECTED_COLOR));
        sidebar.append(readerCategory);
        return sidebar;
    }

    private static LytVBox buildSettingsRows() {
        var rows = new LytVBox();
        rows.setFullWidth(true);
        rows.setFlexGrow(1f);
        rows.setGap(0);
        rows.append(buildFontSizeRow());
        rows.append(buildBodyFontFamilyRow());
        rows.append(buildCodeFontFamilyRow());
        return rows;
    }

    /**
     * Shared stepper-row skeleton: label (flex-grow) on the left,
     * then − / value / +, bottom divider. Every settings row is built through
     * this one method, so the three rows are isomorphic by construction.
     */
    private static LytHBox buildStepperRow(String labelText, String valueText, String minusTooltip, String plusTooltip,
        Consumer<GuideUiHost> onMinus, Consumer<GuideUiHost> onPlus) {
        // Row skeleton mirrors GuideSearchResultDocumentBuilder.ResultRowBlock:
        // full-width, no wrap, stable height via padding + bottom divider.
        var row = new LytHBox();
        row.setFullWidth(true);
        row.setWrap(false);
        row.setGap(8);
        row.setAlignItems(AlignItems.CENTER);
        row.setPaddingBottom(4);
        row.setBorderBottom(new BorderStyle(DIVIDER_COLOR, 1));

        var label = new LytParagraph();
        label.setFlexGrow(1f);
        label.appendText(labelText);

        var value = new LytParagraph();
        value.appendText(valueText);

        var minus = new TextButton("−");
        minus.setOnClick(onMinus);
        minus.setTooltipText(minusTooltip);

        var plus = new TextButton("+");
        plus.setOnClick(onPlus);
        plus.setTooltipText(plusTooltip);

        row.append(label);
        row.append(minus);
        row.append(value);
        row.append(plus);
        return row;
    }

    private static LytHBox buildFontSizeRow() {
        return buildStepperRow(
            "字号 / Font size",
            displayFontSize(ModConfig.reader.readerFontSize),
            "减小字号 / Decrease font size",
            "增大字号 / Increase font size",
            screen -> onFontSizeStep(screen, false),
            screen -> onFontSizeStep(screen, true));
    }

    private static LytHBox buildBodyFontFamilyRow() {
        return buildStepperRow(
            "正文字体 / Font family",
            ModConfig.reader.readerFontFamily,
            "上一个字体 / Previous font family",
            "下一个字体 / Next font family",
            screen -> onBodyFontFamilyStep(screen, false),
            screen -> onBodyFontFamilyStep(screen, true));
    }

    private static LytHBox buildCodeFontFamilyRow() {
        return buildStepperRow(
            "代码字体 / Code font",
            ModConfig.reader.readerCodeFontFamily,
            "上一个字体 / Previous font family",
            "下一个字体 / Next font family",
            screen -> onCodeFontFamilyStep(screen, false),
            screen -> onCodeFontFamilyStep(screen, true));
    }

    /** Applies one preset step to the persisted reader font size (write + save). */
    static void applyFontSizeStep(boolean increase) {
        int current = ModConfig.reader.readerFontSize;
        int next = increase ? FontSizePresets.next(current) : FontSizePresets.prev(current);
        if (next == current) {
            return;
        }
        ModConfig.reader.readerFontSize = next;
        ModConfig.save();
    }

    /** Applies one preset step to the persisted reader body font family (write + save). */
    static void applyBodyFontFamilyStep(boolean increase) {
        String current = ModConfig.reader.readerFontFamily;
        String next = increase ? StringPresets.next(BODY_FONT_FAMILY_PRESETS, current)
            : StringPresets.prev(BODY_FONT_FAMILY_PRESETS, current);
        if (next.equals(current)) {
            return;
        }
        ModConfig.reader.readerFontFamily = next;
        ModConfig.save();
    }

    /** Applies one preset step to the persisted reader code font family (write + save). */
    static void applyCodeFontFamilyStep(boolean increase) {
        String current = ModConfig.reader.readerCodeFontFamily;
        String next = increase ? StringPresets.next(CODE_FONT_FAMILY_PRESETS, current)
            : StringPresets.prev(CODE_FONT_FAMILY_PRESETS, current);
        if (next.equals(current)) {
            return;
        }
        ModConfig.reader.readerCodeFontFamily = next;
        ModConfig.save();
    }

    /**
     * Click wiring: persist the step, then refresh the settings document so the
     * value echo updates. Relayout itself is automatic: the frame-by-frame
     * invalidation in {@code GuideScreen.ensureLayout}, which compares
     * {@code resolveBaseFontSize} every frame, handles it, so no extra
     * invalidation mechanism is added here.
     */
    private static void onFontSizeStep(GuideUiHost screen, boolean increase) {
        applyFontSizeStep(increase);
        if (screen instanceof GuideScreen guideScreen) {
            guideScreen.rebuildSettingsDocument();
        }
    }

    private static void onBodyFontFamilyStep(GuideUiHost screen, boolean increase) {
        applyBodyFontFamilyStep(increase);
        if (screen instanceof GuideScreen guideScreen) {
            guideScreen.rebuildSettingsDocument();
        }
    }

    private static void onCodeFontFamilyStep(GuideUiHost screen, boolean increase) {
        applyCodeFontFamilyStep(increase);
        if (screen instanceof GuideScreen guideScreen) {
            guideScreen.rebuildSettingsDocument();
        }
    }

    public static String displayFontSize(int size) {
        return size == 0 ? "跟随系统" : String.valueOf(size);
    }

    /** Pure stepping helper (endpoints clamp) so the preset walk is unit-testable. */
    public static final class FontSizePresets {

        private FontSizePresets() {}

        public static int next(int current) {
            for (int preset : FONT_SIZE_PRESETS) {
                if (preset > current) {
                    return preset;
                }
            }
            return FONT_SIZE_PRESETS[FONT_SIZE_PRESETS.length - 1];
        }

        public static int prev(int current) {
            for (int i = FONT_SIZE_PRESETS.length - 1; i >= 0; i--) {
                if (FONT_SIZE_PRESETS[i] < current) {
                    return FONT_SIZE_PRESETS[i];
                }
            }
            return FONT_SIZE_PRESETS[0];
        }
    }

    /**
     * Pure string-preset stepping helper shared by both font-family rows.
     * The {@code presets} array order IS the stepping order; endpoints
     * clamp exactly like {@link FontSizePresets}: the font-size row clamps, so
     * the family rows clamp too. A current value absent from the array (e.g. a
     * hand-edited config value) is treated as above-range for {@link #next}
     * (top clamp) and below-range for {@link #prev} (bottom clamp); the string
     * analog of FontSizePresets' out-of-range behavior, since string presets
     * carry no intrinsic total order.
     */
    public static final class StringPresets {

        private StringPresets() {}

        public static String next(String[] presets, String current) {
            for (int i = 0; i < presets.length; i++) {
                if (presets[i].equals(current)) {
                    return i + 1 < presets.length ? presets[i + 1] : presets[i];
                }
            }
            return presets[presets.length - 1];
        }

        public static String prev(String[] presets, String current) {
            for (int i = 0; i < presets.length; i++) {
                if (presets[i].equals(current)) {
                    return i > 0 ? presets[i - 1] : presets[i];
                }
            }
            return presets[0];
        }
    }

    /**
     * Text-rendering stepper button. Sprite-less LytButton (construction is
     * headless-safe); the label is drawn at render time through the standard
     * GuideText pipeline, mirroring LytParagraph's fallback emission.
     */
    public static final class TextButton extends LytButton {

        private final String label;

        public TextButton(String label) {
            super(null, new LytSize(STEP_BUTTON_WIDTH, STEP_BUTTON_HEIGHT));
            this.label = label;
            modifyStyle(style -> style.bold(true));
        }

        public String getLabel() {
            return label;
        }

        @Override
        public void computePrimitives(PrimitiveCollector c) {
            var bounds = getBounds();
            if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0 || label.isEmpty()) {
                return;
            }
            int lineHeight = GuideText.baseLineHeight();
            int textY = bounds.y() + Math.max(0, (bounds.height() - lineHeight) / 2);
            GuideText.emitText(c, label, bounds.x() + 3, textY, resolveStyle());
        }
    }
}
