package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class MediaWikiGeneratedListBlock extends LytBlock implements InteractiveElement, DebugComponent {

    private static final int TOP_PADDING = 6;
    private static final int BOTTOM_PADDING = 6;
    private static final int SIDE_PADDING = 2;
    private static final int COLUMN_GAP = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 20;
    private static final int SECTION_GAP_TOP = 5;
    private static final int SECTION_GAP_BOTTOM = 3;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int LIST_MARKER_SIZE = 3;
    private static final int LIST_MARKER_GAP = 6;
    private static final ConstantColor LIST_MARKER_COLOR = ConstantColor.WHITE;
    private static final ResolvedTextStyle LINK_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.LINK)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle HOVER_LINK_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.LINK)
        .underlined(true)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle HEADER_STYLE = TextStyle.builder()
        .apply(DefaultStyles.HEADING1)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle EMPTY_STYLE = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);

    private final List<MediaWikiListEntry> entries = new ArrayList<>();
    private final List<RowLayout> rowLayouts = new ArrayList<>();
    private int rows = MediaWikiListPlanner.DEFAULT_ROWS;
    private String emptyText = GuidebookText.MediaWikiNoPages.text();
    @Nullable
    private RowLayout hoveredRow;

    /** Precomputed max column content height for Rust MeasureFunc. Set during computeLayout. */
    private int maxPrecomputedContentHeight = 0;

    public void setEntries(List<MediaWikiListEntry> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        // Invalidate precomputed height when entries change
        this.maxPrecomputedContentHeight = 0;
    }

    public void setRows(int rows) {
        this.rows = MediaWikiListPlanner.sanitizeRows(rows);
        // Invalidate precomputed height when row count changes
        this.maxPrecomputedContentHeight = 0;
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText != null && !emptyText.isEmpty() ? emptyText : GuidebookText.MediaWikiNoPages.text();
    }

    /**
     * Returns the precomputed max column content height (tallest column's content,
     * excluding TOP_PADDING and BOTTOM_PADDING). Used by the Rust MeasureFunc.
     * Equals ROW_HEIGHT when entries is empty.
     * <p>
     * Computed lazily when no layout pass has been run (value is 0). The
     * calculation depends only on block fields ({@link #entries}, {@link #rows})
     * and constants — not on a {@link LayoutContext} — so it works without the
     * Java layout pre-pass.
     */
    public int getMaxPrecomputedContentHeight() {
        if (maxPrecomputedContentHeight <= 0) {
            maxPrecomputedContentHeight = computeMaxPrecomputedContentHeight();
        }
        return maxPrecomputedContentHeight;
    }

    /**
     * Computes the max column content height from block state alone (no
     * {@link LayoutContext} needed). The height is independent of availableWidth
     * because column packing uses <em>row count</em> (not pixel width) to
     * distribute entries; each entry occupies a fixed {@link #ROW_HEIGHT}.
     */
    private int computeMaxPrecomputedContentHeight() {
        if (entries.isEmpty()) {
            return ROW_HEIGHT;
        }
        int columnCount = Math.max(1, rows);
        List<MediaWikiListPlanner.MediaWikiListColumn> columns = MediaWikiListPlanner.planColumns(entries, columnCount);
        int maxColumnHeight = 0;
        for (MediaWikiListPlanner.MediaWikiListColumn column : columns) {
            int columnHeight = 0;
            for (MediaWikiListPlanner.MediaWikiListSection section : column.sections()) {
                if (!section.entries()
                    .isEmpty()) {
                    columnHeight += SECTION_GAP_TOP;
                }
                columnHeight += HEADER_HEIGHT + SECTION_GAP_BOTTOM;
                columnHeight += section.entries()
                    .size() * ROW_HEIGHT;
            }
            maxColumnHeight = Math.max(maxColumnHeight, columnHeight);
        }
        return maxColumnHeight;
    }

    @Override
    protected void afterExternalLayout() {
        // Recompute row layouts from Rust-computed bounds (the Java pre-pass
        // no longer calls computeLayout). Uses only block fields and constants
        // — no LayoutContext needed (heights are fixed constants).
        if (bounds.isEmpty()) return;
        recomputeRowLayouts(bounds.x(), bounds.y(), bounds.width());
    }

    /** Shared row-layout computation used by both the pre-pass and afterExternalLayout. */
    private void recomputeRowLayouts(int x, int y, int availableWidth) {
        rowLayouts.clear();
        hoveredRow = null;
        int columnCount = Math.max(1, rows);
        int innerWidth = Math.max(0, availableWidth - SIDE_PADDING * 2);
        int columnWidth = Math.max(1, (innerWidth - COLUMN_GAP * (columnCount - 1)) / columnCount);
        if (entries.isEmpty()) {
            rowLayouts
                .add(new RowLayout(new LytRect(x + SIDE_PADDING, y + TOP_PADDING, innerWidth, ROW_HEIGHT), null, null));
            return;
        }
        List<MediaWikiListPlanner.MediaWikiListColumn> columns = MediaWikiListPlanner.planColumns(entries, columnCount);
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            int columnX = x + SIDE_PADDING + columnIndex * (columnWidth + COLUMN_GAP);
            int columnY = y + TOP_PADDING;
            for (var section : columns.get(columnIndex)
                .sections()) {
                if (!section.entries()
                    .isEmpty()) columnY += SECTION_GAP_TOP;
                rowLayouts
                    .add(new RowLayout(new LytRect(columnX, columnY, columnWidth, HEADER_HEIGHT), null, section.key()));
                columnY += HEADER_HEIGHT + SECTION_GAP_BOTTOM;
                for (MediaWikiListEntry entry : section.entries()) {
                    rowLayouts.add(new RowLayout(new LytRect(columnX, columnY, columnWidth, ROW_HEIGHT), entry, null));
                    columnY += ROW_HEIGHT;
                }
            }
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        recomputeRowLayouts(x, y, availableWidth);
        // maxPrecomputedContentHeight has a lazy getter — it will be
        // computed on demand if not set here.
        return new LytRect(x, y, availableWidth, TOP_PADDING + getMaxPrecomputedContentHeight() + BOTTOM_PADDING);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        rowLayouts.replaceAll(layout -> layout.move(deltaX, deltaY));
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        emitBorders(c);
        for (RowLayout rowLayout : rowLayouts) {
            if (c.isCulled(rowLayout.bounds())) {
                continue;
            }

            MediaWikiListEntry entry = rowLayout.entry();
            if (entry == null && rowLayout.header() == null) {
                rowLayout.setClickableBounds(LytRect.empty());
                int emptyTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        EMPTY_STYLE,
                        rowLayout.bounds()
                            .height());
                emitText(
                    c,
                    emptyText,
                    rowLayout.bounds()
                        .x(),
                    emptyTextY,
                    EMPTY_STYLE);
                continue;
            }

            if (rowLayout.header() != null) {
                int headerTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        HEADER_STYLE,
                        rowLayout.bounds()
                            .height());
                emitText(
                    c,
                    rowLayout.header(),
                    rowLayout.bounds()
                        .x(),
                    headerTextY,
                    HEADER_STYLE);
                rowLayout.setClickableBounds(LytRect.empty());
                continue;
            }

            ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
            int rowTextY = rowLayout.bounds()
                .y()
                + verticalCenterOffset(
                    rowStyle,
                    rowLayout.bounds()
                        .height());
            int markerX = rowLayout.bounds()
                .x();
            int markerY = rowLayout.bounds()
                .y()
                + (rowLayout.bounds()
                    .height() - LIST_MARKER_SIZE) / 2;
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    markerX,
                    markerY,
                    LIST_MARKER_SIZE,
                    LIST_MARKER_SIZE,
                    LIST_MARKER_COLOR.resolve(LightDarkMode.current())));
            int textX = markerX + LIST_MARKER_SIZE + LIST_MARKER_GAP;
            GuidePageIcon icon = entry.icon();
            if (icon != null) {
                emitIcon(
                    c,
                    icon,
                    textX,
                    rowLayout.bounds()
                        .y()
                        + (rowLayout.bounds()
                            .height() - ICON_SIZE) / 2);
                textX += ICON_SIZE + ICON_GAP;
            }

            String renderedTitle = GuideText.clipToWidth(
                entry.title(),
                Math.max(
                    1,
                    rowLayout.bounds()
                        .right() - textX),
                rowStyle,
                GuideText.ClipSuffix.DOTS3);
            int clickableWidth = textX - rowLayout.bounds()
                .x();
            if (renderedTitle != null && !renderedTitle.isEmpty()) {
                clickableWidth += GuideText.measureWidth(renderedTitle, rowStyle);
            }
            rowLayout.setClickableBounds(
                new LytRect(
                    rowLayout.bounds()
                        .x(),
                    rowLayout.bounds()
                        .y(),
                    Math.max(0, clickableWidth),
                    rowLayout.bounds()
                        .height()));
            emitText(c, renderedTitle, textX, rowTextY, rowStyle);
        }
    }

    /**
     * Migrated to {@link #computePrimitives}; the legacy path is unreachable
     * (the collector only invokes it when {@link #usePrimitives()} is false).
     */
    @Override
    public void render(RenderContext context) {}

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }

        for (RowLayout rowLayout : rowLayouts) {
            if (!rowLayout.clickableBounds()
                .contains(x, y)) {
                continue;
            }
            MediaWikiListEntry entry = rowLayout.entry();
            if (entry == null) {
                return false;
            }
            screen.navigateTo(PageAnchor.page(entry.pageId()));
            return true;
        }
        return false;
    }

    @Override
    public void onMouseEnter(@Nullable LytFlowContent hoveredContent) {
        hoveredRow = null;
    }

    @Override
    public void onMouseLeave() {
        hoveredRow = null;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        hoveredRow = findClickableRow((int) x, (int) y);
        return Optional.empty();
    }

    @Nullable
    private RowLayout findClickableRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.entry() != null && rowLayout.clickableBounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return null;
    }

    private void emitIcon(PrimitiveCollector c, GuidePageIcon icon, int x, int y) {
        if (icon.isItemIcon() && icon.resolveCurrentItemStack() != null) {
            c.emit(new GuideRenderPrimitive.RenderItem(icon.resolveCurrentItemStack(), x, y));
            return;
        }
        GuidePageTexture texture = icon.resolveCurrentTexture();
        if (texture == null || texture.isMissing()) {
            return;
        }
        ResourceLocation resolvedTexture = texture.getTexture();
        int texId = resolvedTexture != null ? getGlTextureId(resolvedTexture) : -1;
        if (texId >= 0) {
            c.emit(new GuideRenderPrimitive.BlitTexture(texId, x, y, ICON_SIZE, ICON_SIZE, 0f, 0f, 1f, 1f));
        }
    }

    private int verticalCenterOffset(ResolvedTextStyle style, int boxHeight) {
        return Math.max(0, (boxHeight - GuideText.lineHeight(style)) / 2);
    }

    private void emitBorders(PrimitiveCollector c) {
        if (getBorderTop().width() <= 0 && getBorderLeft().width() <= 0
            && getBorderRight().width() <= 0
            && getBorderBottom().width() <= 0) {
            return;
        }
        c.emit(
            new GuideRenderPrimitive.DrawBorder(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                getBorderTop().width(),
                getBorderLeft().width(),
                getBorderBottom().width(),
                getBorderRight().width(),
                resolveBorderArgb()));
    }

    private int resolveBorderArgb() {
        // DrawBorder is single-color; use the first side that declares one.
        // This block's callers set top+bottom with the same color
        // (SymbolicColor.TABLE_BORDER), so the single color is exact here.
        BorderStyle[] sides = { getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom() };
        for (BorderStyle side : sides) {
            var color = side.color();
            if (color != null) {
                return color.resolve(LightDarkMode.current());
            }
        }
        return 0xFF000000;
    }

    /**
     * Emits {@code text} through the unified {@link GuideText} glyph pipeline
     * at document position {@code (x, lineTop)} — {@code lineTop} is the line
     * top (baseline = lineTop + ascent × fontScale), not the MC baseline.
     * <p>
     * GuideText does not paint decorations; the underline of
     * {@link #HOVER_LINK_STYLE} is therefore drawn manually as a 1px
     * {@link GuideRenderPrimitive.FillRect} 1px below the baseline, spanning
     * the measured text width (same width the glyph run occupies).
     */
    private static void emitText(PrimitiveCollector c, String text, int x, int lineTop, ResolvedTextStyle style) {
        GuideText.emitText(c, text, x, lineTop, style);
        if (style != null && style.underlined() && text != null && !text.isEmpty()) {
            int width = GuideText.measureWidth(text, style);
            int underlineY = Math.round(GuideText.baselineOf(lineTop, style)) + 1;
            c.emit(new GuideRenderPrimitive.FillRect(x, underlineY, width, 1, GuideText.resolveColor(style)));
        }
    }

    private static int getGlTextureId(ResourceLocation res) {
        try {
            ITextureObject tex = Minecraft.getMinecraft()
                .getTextureManager()
                .getTexture(res);
            return tex != null ? tex.getGlTextureId() : -1;
        } catch (Throwable t) {
            // Headless (unit tests) or texture unavailable: skip drawing.
            return -1;
        }
    }

    private static class RowLayout {

        private final LytRect bounds;
        @Nullable
        private final MediaWikiListEntry entry;
        @Nullable
        private final String header;
        private LytRect clickableBounds;

        private RowLayout(LytRect bounds, @Nullable MediaWikiListEntry entry, @Nullable String header) {
            this.bounds = bounds;
            this.entry = entry;
            this.header = header;
            this.clickableBounds = LytRect.empty();
        }

        private LytRect bounds() {
            return bounds;
        }

        @Nullable
        private MediaWikiListEntry entry() {
            return entry;
        }

        @Nullable
        private String header() {
            return header;
        }

        private LytRect clickableBounds() {
            return clickableBounds;
        }

        private void setClickableBounds(LytRect clickableBounds) {
            this.clickableBounds = clickableBounds != null ? clickableBounds : LytRect.empty();
        }

        private RowLayout move(int deltaX, int deltaY) {
            RowLayout moved = new RowLayout(bounds.move(deltaX, deltaY), entry, header);
            moved.setClickableBounds(clickableBounds.move(deltaX, deltaY));
            return moved;
        }
    }

    // Debug implementation

    @Override
    public List<DebugComponent.ComponentEntry> getDebugComponents() {
        List<DebugComponent.ComponentEntry> components = new ArrayList<>();

        for (int i = 0; i < rowLayouts.size(); i++) {
            RowLayout row = rowLayouts.get(i);

            if (row.entry() != null) {
                // Entry row
                String title = row.entry()
                    .title();
                if (title == null || title.isEmpty()) {
                    title = "Entry" + i;
                }
                String extra = "PageID: " + row.entry()
                    .pageId();
                components.add(
                    new DebugComponent.SimpleComponentEntry("ListEntry:" + title, row.clickableBounds(), extra, 10));
            } else if (row.header() != null) {
                // Header row
                components.add(
                    new DebugComponent.SimpleComponentEntry(
                        "ListHeader:" + row.header(),
                        row.bounds(),
                        "Section header",
                        5));
            } else {
                // Empty row
                components.add(new DebugComponent.SimpleComponentEntry("EmptyListRow", row.bounds(), "No entries", 5));
            }
        }

        return components;
    }
}
