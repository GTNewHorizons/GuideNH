package com.hfstudio.guidenh.guide.mediawiki;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
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

public class MediaWikiSpecialGeneratedBlock extends LytBlock implements InteractiveElement {

    private static final int SPECIAL_PAGES_GROUP_COLUMNS = 2;
    private static final int TOP_PADDING = 6;
    private static final int BOTTOM_PADDING = 6;

    /** Precomputed max column content height for Rust MeasureFunc. Set during computeLayout. */
    private int maxPrecomputedContentHeight = 0;

    /** Font facts collected during computeLayout, consumed by Rust measure via serialization. */
    @Nullable
    private FontFacts collectedFontFacts;

    /**
     * Returns the font facts, collecting them on demand if no prior layout has
     * cached them. Called by the serializer (which runs after computeLayout and
     * does NOT have a LayoutContext). Uses {@link GuideText} static methods so
     * no {@link LayoutContext} is required.
     */
    public FontFacts getCollectedFontFacts() {
        if (collectedFontFacts == null) {
            collectedFontFacts = collectFontFacts();
        }
        return collectedFontFacts;
    }

    // ── Serialization helpers for Rust-side measurement ────────────────────

    /**
     * Populates a FontFactsCollector with width-independent font-measurement
     * data consumed by the Rust measure function.
     */
    public FontFacts collectFontFacts() {
        return new FontFacts(collectFontFactsImpl());
    }

    /**
     * Contains all per-entry font facts plus column-planning data for Rust.
     * Exposed as a single object so the serializer calls one method.
     */
    public record FontFacts(FontFactsImpl impl) {
        public record FontFactsImpl(
            int columnCount,
            boolean hasMore,
            int groupCount,
            float[] groupTitleWidths,
            int[] groupEntryCounts,
            float[] groupEstimatedHeights,
            int totalEntryCount,
            float[] entryTitleWidths,
            byte[] entryHasIcon,
            float[] entryEstimatedHeights,
            int[] entrySubtitleLineCounts,
            int[] subtitleLineWordCounts,
            float[] subtitleWordWidths,
            float subtitleSpaceWidth) {}
    }

    private FontFacts.FontFactsImpl collectFontFactsImpl() {
        MediaWikiSpecialPageResult visibleResult = applyVisibility(result, searchQuery);
        int columnCount = resolveColumnCount(visibleResult);
        boolean hasMoreFlag = visibleResult != null && visibleResult.hasMore();
        boolean isGrouped = visibleResult != null
            && (visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
                || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX);

        if (isEmpty(visibleResult)) {
            return new FontFacts.FontFactsImpl(
                columnCount, false, 0, new float[0], new int[0], new float[0],
                0, new float[0], new byte[0], new float[0], new int[0],
                new int[0], new float[0], 0f);
        }

        if (!isGrouped) {
            // ── Flat entries: distribute evenly ──
            List<MediaWikiSpecialListEntry> entries = visibleResult.flatEntries() != null
                ? visibleResult.flatEntries() : List.of();
            int totalEntryCount = entries.size();
            int perColumn = Math.max(1, (totalEntryCount + columnCount - 1) / columnCount);
            // Build per-column groups (one group per column, no title).
            int actualGroupCount = 0;
            for (int ci = 0; ci < columnCount; ci++) {
                int start = ci * perColumn;
                if (start >= totalEntryCount) break;
                actualGroupCount++;
            }
            int[] groupEntryCounts = new int[actualGroupCount];
            float[] groupTitleWidths = new float[actualGroupCount];
            float[] groupEstimatedHeights = new float[actualGroupCount];
            int entryIdx = 0;
            for (int gi = 0; gi < actualGroupCount; gi++) {
                int start = gi * perColumn;
                int end = Math.min(entries.size(), start + perColumn);
                int cnt = end - start;
                groupEntryCounts[gi] = cnt;
                groupTitleWidths[gi] = 0f; // no title
                // Estimate height for this group's entries.
                float estH = 0f;
                for (int ei = start; ei < end; ei++) {
                    estH += estimateEntryHeight(entries.get(ei)) + ENTRY_GAP;
                }
                estH += GROUP_MARGIN;
                groupEstimatedHeights[gi] = estH;
            }

            float[] entryTitleWidths = new float[totalEntryCount];
            byte[] entryHasIcon = new byte[totalEntryCount];
            float[] entryEstimatedHeights = new float[totalEntryCount];
            int[] entrySubtitleLineCounts = new int[totalEntryCount];
            java.util.ArrayList<Integer> lineWordCounts = new java.util.ArrayList<>();
            java.util.ArrayList<Float> wordWidths = new java.util.ArrayList<>();
            float spaceWidth = 0f;

            for (int ei = 0; ei < totalEntryCount; ei++) {
                MediaWikiSpecialListEntry e = entries.get(ei);
                entryTitleWidths[ei] = GuideText.measureWidth(e.title() != null ? e.title() : "", LINK_STYLE);
                entryHasIcon[ei] = (byte) (e.icon() != null ? 1 : 0);
                entryEstimatedHeights[ei] = estimateEntryHeight(e);
                // Subtitle word data
                String sub = e.subtitle();
                if (sub == null || sub.isEmpty()) {
                    entrySubtitleLineCounts[ei] = 0;
                } else {
                    java.util.List<String> rawLines = GuideStringLines.splitLines(sub);
                    entrySubtitleLineCounts[ei] = rawLines.size();
                    for (String rawLine : rawLines) {
                        String trimmed = rawLine.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        String[] words = trimmed.split("\\s+");
                        lineWordCounts.add(words.length);
                        for (String word : words) {
                            if (word.isEmpty()) continue;
                            float w = GuideText.measureWidth(word, SUBTITLE_STYLE);
                            wordWidths.add(w);
                        }
                    }
                }
            }
            // Measure space width from the first subtitle word if available, else default.
            spaceWidth = GuideText.measureWidth(" ", SUBTITLE_STYLE);
            if (spaceWidth <= 0f) spaceWidth = 4f; // fallback

            int[] lwc = new int[lineWordCounts.size()];
            for (int i = 0; i < lwc.length; i++) lwc[i] = lineWordCounts.get(i);
            float[] ww = new float[wordWidths.size()];
            for (int i = 0; i < ww.length; i++) ww[i] = wordWidths.get(i);

            return new FontFacts.FontFactsImpl(
                columnCount, hasMoreFlag, actualGroupCount,
                groupTitleWidths, groupEntryCounts, groupEstimatedHeights,
                totalEntryCount,
                entryTitleWidths, entryHasIcon, entryEstimatedHeights, entrySubtitleLineCounts,
                lwc, ww, spaceWidth);
        } else {
            // ── Grouped entries: one group per result group ──
            java.util.List<GroupLayout> groups = buildGroups(visibleResult);
            int groupCount = groups.size();

            float[] groupTitleWidths = new float[groupCount];
            int[] groupEntryCounts = new int[groupCount];
            float[] groupEstimatedHeights = new float[groupCount];

            // First pass: compute per-group metadata and total entry count.
            int totalEntryCount = 0;
            for (int gi = 0; gi < groupCount; gi++) {
                GroupLayout g = groups.get(gi);
                float tw = 0f;
                if (g.title() != null && !g.title().isEmpty()) {
                    tw = GuideText.measureWidth(g.title(), HEADER_STYLE);
                }
                groupTitleWidths[gi] = tw;
                groupEntryCounts[gi] = g.entries().size();
                totalEntryCount += g.entries().size();
                groupEstimatedHeights[gi] = estimateHeight(g);
            }

            float[] entryTitleWidths = new float[totalEntryCount];
            byte[] entryHasIcon = new byte[totalEntryCount];
            float[] entryEstimatedHeights = new float[totalEntryCount];
            int[] entrySubtitleLineCounts = new int[totalEntryCount];
            java.util.ArrayList<Integer> lineWordCounts = new java.util.ArrayList<>();
            java.util.ArrayList<Float> wordWidths = new java.util.ArrayList<>();
            float spaceWidth = 0f;
            int entryIdx = 0;

            for (int gi = 0; gi < groupCount; gi++) {
                GroupLayout g = groups.get(gi);
                for (MediaWikiSpecialListEntry e : g.entries()) {
                    entryTitleWidths[entryIdx] = GuideText.measureWidth(e.title() != null ? e.title() : "", LINK_STYLE);
                    entryHasIcon[entryIdx] = (byte) (e.icon() != null ? 1 : 0);
                    entryEstimatedHeights[entryIdx] = estimateEntryHeight(e);

                    String sub = e.subtitle();
                    if (sub == null || sub.isEmpty()) {
                        entrySubtitleLineCounts[entryIdx] = 0;
                    } else {
                        java.util.List<String> rawLines = GuideStringLines.splitLines(sub);
                        entrySubtitleLineCounts[entryIdx] = rawLines.size();
                        for (String rawLine : rawLines) {
                            String trimmed = rawLine.trim();
                            if (trimmed.isEmpty()) continue;
                            String[] words = trimmed.split("\\s+");
                            lineWordCounts.add(words.length);
                            for (String word : words) {
                                if (word.isEmpty()) continue;
                                float w = GuideText.measureWidth(word, SUBTITLE_STYLE);
                                wordWidths.add(w);
                            }
                        }
                    }
                    entryIdx++;
                }
            }

            spaceWidth = GuideText.measureWidth(" ", SUBTITLE_STYLE);
            if (spaceWidth <= 0f) spaceWidth = 4f;

            int[] lwc = new int[lineWordCounts.size()];
            for (int i = 0; i < lwc.length; i++) lwc[i] = lineWordCounts.get(i);
            float[] ww = new float[wordWidths.size()];
            for (int i = 0; i < ww.length; i++) ww[i] = wordWidths.get(i);

            return new FontFacts.FontFactsImpl(
                columnCount, hasMoreFlag, groupCount,
                groupTitleWidths, groupEntryCounts, groupEstimatedHeights,
                totalEntryCount,
                entryTitleWidths, entryHasIcon, entryEstimatedHeights, entrySubtitleLineCounts,
                lwc, ww, spaceWidth);
        }
    }
    private static final int SIDE_PADDING = 2;
    private static final int COLUMN_GAP = 10;
    private static final int GROUP_MARGIN = 6;
    private static final int HEADER_HEIGHT = 20;
    private static final int ENTRY_HEIGHT = 20;
    private static final int ENTRY_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int LIST_MARKER_SIZE = 3;
    private static final int LIST_MARKER_GAP = 6;
    private static final int TITLE_SUBTITLE_GAP = 3;
    private static final int ENTRY_VERTICAL_PADDING_TOP = 3;
    private static final int ENTRY_VERTICAL_PADDING_BOTTOM = 3;
    private static final int HEADER_MARGIN_TOP = 5;
    private static final int HEADER_MARGIN_BOTTOM = 5;
    private static final int LOAD_MORE_HEIGHT = 18;
    private static final int LOAD_MORE_MARGIN_TOP = 2;
    private static final String ELLIPSIS = "...";
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
    private static final ResolvedTextStyle SUBTITLE_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.GRAY)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle EMPTY_STYLE = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);

    private final List<RowLayout> rowLayouts = new ArrayList<>();
    private MediaWikiSpecialPageResult result = MediaWikiSpecialPageModels.info(
        new MediaWikiSpecialDefinition(
            "Special",
            "guidenh.mediawiki.special.unsupported",
            "other",
            MediaWikiSpecialPageKind.INFO,
            false,
            false,
            MediaWikiSpecialPageQuery.PAGE_SIZE,
            null),
        "");
    private String searchQuery = "";
    private int rows = MediaWikiListPlanner.DEFAULT_ROWS;
    private String emptyText = GuidebookText.MediaWikiNoPages.text();
    private int currentVisibleCount = MediaWikiSpecialPageQuery.PAGE_SIZE;
    @Nullable
    private VisibilityCache visibilityCache;
    @Nullable
    private RowLayout hoveredRow;
    @Nullable
    private MediaWikiListContext listContext;
    @Nullable
    private MediaWikiSpecialDefinition definition;
    @Nullable
    private MediaWikiSpecialPageResolver resolver;
    @Nullable
    private Map<String, String> queryParameters;

    public void setResult(MediaWikiSpecialPageResult result) {
        if (result != null) {
            this.result = result;
            definition = result.definition();
            visibilityCache = null;
            currentVisibleCount = resolveDefaultVisibleCount(result);
            maxPrecomputedContentHeight = 0;
            collectedFontFacts = null;
        }
    }

    public void setResolverContext(MediaWikiListContext listContext, MediaWikiSpecialDefinition definition,
        MediaWikiSpecialPageResolver resolver, @Nullable Map<String, String> queryParameters) {
        this.listContext = listContext;
        this.definition = definition;
        this.resolver = resolver;
        this.queryParameters = queryParameters != null && !queryParameters.isEmpty()
            ? new LinkedHashMap<>(queryParameters)
            : Map.of();
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, rows);
        maxPrecomputedContentHeight = 0;
        collectedFontFacts = null;
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText != null && !emptyText.isEmpty() ? emptyText : GuidebookText.MediaWikiNoPages.text();
    }

    /**
     * Returns the precomputed max column content height (tallest column's content,
     * excluding TOP_PADDING and BOTTOM_PADDING). Used by the Rust MeasureFunc.
     * Equals ENTRY_HEIGHT when the visible result is empty.
     * <p>
     * Computed lazily when the cached value is zero (no layout pre-pass has run,
     * or state was invalidated). Uses {@link #estimateEntryHeight} so no
     * {@link LayoutContext} is needed.
     */
    public int getMaxPrecomputedContentHeight() {
        if (maxPrecomputedContentHeight <= 0) {
            maxPrecomputedContentHeight = computeMaxPrecomputedContentHeight();
        }
        return maxPrecomputedContentHeight;
    }

    /**
     * Computes max column content height from block state alone, without
     * requiring a {@link LayoutContext}. Falls back to {@link #estimateEntryHeight}
     * for entry heights (which does not need text-width measurement).
     */
    private int computeMaxPrecomputedContentHeight() {
        MediaWikiSpecialPageResult visibleResult = applyVisibility(result, searchQuery);
        int columnCount = resolveColumnCount(visibleResult);
        int innerWidth = 0; // not used for height calculation with estimateEntryHeight

        if (isEmpty(visibleResult)) {
            return ENTRY_HEIGHT;
        }

        List<List<GroupLayout>> columns = layoutColumns(visibleResult);
        int maxColumnHeight = 0;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            int columnY = 0;
            for (GroupLayout group : columns.get(columnIndex)) {
                if (group.title() != null) {
                    columnY += HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM;
                }
                for (MediaWikiSpecialListEntry entry : group.entries()) {
                    columnY += estimateEntryHeight(entry);
                    columnY += ENTRY_GAP;
                }
                columnY += GROUP_MARGIN;
            }
            maxColumnHeight = Math.max(maxColumnHeight, columnY);
        }
        if (visibleResult.hasMore()) {
            maxColumnHeight += LOAD_MORE_MARGIN_TOP + LOAD_MORE_HEIGHT;
        }
        return maxColumnHeight;
    }

    public void setSearchQuery(String searchQuery) {
        String nextSearchQuery = searchQuery != null ? searchQuery : "";
        if (this.searchQuery.equals(nextSearchQuery)) {
            return;
        }
        this.searchQuery = nextSearchQuery;
        if (supportsResolverBackedSearch()) {
            refreshResolverBackedResult(nextSearchQuery);
        }
        if (MediaWikiSpecialSearchSupport.normalize(nextSearchQuery)
            .isEmpty()) {
            currentVisibleCount = resolveDefaultVisibleCount(result);
        }
        visibilityCache = null;
        maxPrecomputedContentHeight = 0;
        collectedFontFacts = null;
        var document = getDocument();
        if (document != null) {
            document.invalidateLayout();
        }
    }

    private boolean supportsResolverBackedSearch() {
        return listContext != null && definition != null
            && resolver != null
            && definition.name() != null
            && (MediaWikiSpecialPageIds.PAGE_TRANSLATION.equals(definition.name())
                || MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS.equals(definition.name())
                || MediaWikiSpecialPageIds.ALL_TRANSLATIONS.equals(definition.name())
                || MediaWikiSpecialPageIds.ALL_PAGES_WITH_PREFIX.equals(definition.name()));
    }

    private void refreshResolverBackedResult(String queryText) {
        if (!supportsResolverBackedSearch()) {
            return;
        }
        MediaWikiSpecialPageQuery query = new MediaWikiSpecialPageQuery(
            queryText != null ? queryText : "",
            Integer.MAX_VALUE,
            queryParameters != null ? queryParameters : Map.of());
        MediaWikiSpecialPageResult refreshed = resolver.resolve(listContext, definition, query);
        if (refreshed != null) {
            result = refreshed;
            definition = refreshed.definition();
        }
    }

    @Override
    protected void afterExternalLayout() {
        // Recompute row layouts from Rust-computed bounds (the Java pre-pass
        // no longer calls computeLayout). Uses computeEntryHeight with a
        // GuideText fallback LayoutContext (no real FontMetrics available).
        if (bounds.isEmpty()) return;
        recomputeRowLayouts(bounds.x(), bounds.y(), bounds.width());
    }

    /** Shared row-layout computation for both the pre-pass and afterExternalLayout. */
    private void recomputeRowLayouts(int x, int y, int availableWidth) {
        rowLayouts.clear();
        hoveredRow = null;
        MediaWikiSpecialPageResult visibleResult = applyVisibility(result, searchQuery);
        int columnCount = resolveColumnCount(visibleResult);
        int innerWidth = Math.max(0, availableWidth - SIDE_PADDING * 2);
        int columnWidth = Math.max(1, (innerWidth - COLUMN_GAP * (columnCount - 1)) / columnCount);
        if (isEmpty(visibleResult)) {
            rowLayouts.add(
                new RowLayout(new LytRect(x + SIDE_PADDING, y + TOP_PADDING, innerWidth, ENTRY_HEIGHT), null));
            return;
        }
        List<List<GroupLayout>> columns = layoutColumns(visibleResult);
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            int columnX = x + SIDE_PADDING + columnIndex * (columnWidth + COLUMN_GAP);
            int columnY = y + TOP_PADDING;
            for (GroupLayout group : columns.get(columnIndex)) {
                if (group.title() != null) {
                    rowLayouts.add(
                        new RowLayout(
                            new LytRect(columnX, columnY + HEADER_MARGIN_TOP, columnWidth, HEADER_HEIGHT),
                            new RenderRow(group.title(), "", null, null, null, null, true, false)));
                    columnY += HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM;
                }
                for (MediaWikiSpecialListEntry entry : group.entries()) {
                    int entryHeight = computeEntryHeight(
                        new LayoutContext(new FontMetrics() {
                            @Override
                            public float getAdvance(int cp, ResolvedTextStyle s) {
                                return GuideText.measureWidth(new String(Character.toChars(cp)), s);
                            }
                            @Override
                            public int getLineHeight(ResolvedTextStyle s) {
                                return GuideText.lineHeight(s);
                            }
                        }), entry, columnWidth);
                    rowLayouts.add(
                        new RowLayout(
                            new LytRect(columnX, columnY, columnWidth, entryHeight),
                            new RenderRow(
                                entry.title(),
                                entry.subtitle(),
                                entry.pageId(),
                                entry.lineNumber(),
                                entry.icon(),
                                entry.externalUrl(),
                                false,
                                false)));
                    columnY += entryHeight + ENTRY_GAP;
                }
                columnY += GROUP_MARGIN;
            }
        }
        if (visibleResult.hasMore()) {
            int maxColumnHeight = rowLayouts.stream()
                .mapToInt(rl -> rl.bounds().bottom() - y)
                .max().orElse(0);
            rowLayouts.add(
                new RowLayout(
                    new LytRect(
                        x + SIDE_PADDING,
                        y + TOP_PADDING + maxColumnHeight + LOAD_MORE_MARGIN_TOP,
                        innerWidth,
                        LOAD_MORE_HEIGHT),
                    RenderRow.loadMore()));
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        recomputeRowLayouts(x, y, availableWidth);
        // maxPrecomputedContentHeight has a lazy getter — it will be
        // computed on demand if not set here.
        return new LytRect(x, y, availableWidth,
            TOP_PADDING + getMaxPrecomputedContentHeight() + BOTTOM_PADDING);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        rowLayouts.replaceAll(rowLayout -> rowLayout.move(deltaX, deltaY));
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

            RenderRow row = rowLayout.row();
            if (row == null) {
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

            if (row.header()) {
                rowLayout.setClickableBounds(LytRect.empty());
                String renderedHeader = clipToWidth(
                    row.title(),
                    rowLayout.bounds()
                        .width(),
                    HEADER_STYLE);
                int headerTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        HEADER_STYLE,
                        rowLayout.bounds()
                            .height());
                emitText(
                    c,
                    renderedHeader,
                    rowLayout.bounds()
                        .x(),
                    headerTextY,
                    HEADER_STYLE);
                rowLayout.setTooltip(isClipped(renderedHeader, row.title()) ? new TextTooltip(row.title()) : null);
                continue;
            }

            if (row.loadMoreRow()) {
                ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
                int loadMoreY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        rowStyle,
                        rowLayout.bounds()
                            .height());
                emitText(
                    c,
                    GuidebookText.SpecialPageShowMore.text(),
                    rowLayout.bounds()
                        .x(),
                    loadMoreY,
                    rowStyle);
                rowLayout.setClickableBounds(rowLayout.bounds());
                continue;
            }

            ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
            int textMaxWidth = Math.max(
                1,
                rowLayout.bounds()
                    .right()
                    - rowLayout.bounds()
                        .x()
                    - LIST_MARKER_SIZE
                    - LIST_MARKER_GAP
                    - (row.icon() != null ? ICON_SIZE + ICON_GAP : 0));
            List<String> subtitleLines = wrapLines(row.subtitle(), textMaxWidth, SUBTITLE_STYLE);
            int markerX = rowLayout.bounds()
                .x();
            int contentTop = rowLayout.bounds()
                .y() + rowContentTop(rowStyle, subtitleLines);
            int contentHeight = rowContentHeight(rowStyle, subtitleLines);
            int markerY = contentTop + Math.max(0, (contentHeight - LIST_MARKER_SIZE) / 2);
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    markerX,
                    markerY,
                    LIST_MARKER_SIZE,
                    LIST_MARKER_SIZE,
                    LIST_MARKER_COLOR.resolve(LightDarkMode.current())));

            int textX = markerX + LIST_MARKER_SIZE + LIST_MARKER_GAP;
            if (row.icon() != null) {
                emitIcon(c, row.icon(), textX, contentTop + Math.max(0, (contentHeight - ICON_SIZE) / 2));
                textX += ICON_SIZE + ICON_GAP;
            }
            String renderedTitle = clipToWidth(
                row.title(),
                Math.max(
                    1,
                    rowLayout.bounds()
                        .right() - textX),
                rowStyle);
            String fullTitle = row.title() != null ? row.title() : "";
            boolean titleClipped = isClipped(renderedTitle, fullTitle);
            String fullSubtitle = row.subtitle() != null ? row.subtitle() : "";
            boolean hasSubtitle = !fullSubtitle.isEmpty();
            int titleY = hasSubtitle ? contentTop
                : rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        rowStyle,
                        rowLayout.bounds()
                            .height());
            emitText(c, renderedTitle, textX, titleY, rowStyle);

            int clickableWidth = textX - rowLayout.bounds()
                .x();
            if (renderedTitle != null && !renderedTitle.isEmpty()) {
                clickableWidth += GuideText.measureWidth(renderedTitle, rowStyle);
            }
            int clickableHeight = GuideText.lineHeight(rowStyle);

            if (row.subtitle() != null && !row.subtitle()
                .isEmpty()) {
                boolean subtitleClipped = areLinesClipped(subtitleLines, fullSubtitle);
                int subtitleY = contentTop + GuideText.lineHeight(rowStyle) + TITLE_SUBTITLE_GAP;
                for (String subtitleLine : subtitleLines) {
                    emitText(c, subtitleLine, textX, subtitleY, SUBTITLE_STYLE);
                    clickableWidth = Math.max(
                        clickableWidth,
                        textX - rowLayout.bounds()
                            .x() + GuideText.measureWidth(subtitleLine, SUBTITLE_STYLE));
                    subtitleY += GuideText.lineHeight(SUBTITLE_STYLE);
                }
                clickableWidth = Math.max(
                    clickableWidth,
                    textX - rowLayout.bounds()
                        .x());
                clickableHeight = rowContentHeight(rowStyle, subtitleLines);
                rowLayout.setTooltip(
                    titleClipped || subtitleClipped ? new TextTooltip(fullTitle + "\n" + fullSubtitle)
                        : titleClipped ? new TextTooltip(fullTitle)
                            : subtitleClipped ? new TextTooltip(fullSubtitle) : null);
            } else {
                rowLayout.setTooltip(titleClipped ? new TextTooltip(fullTitle) : null);
            }

            rowLayout.setClickableBounds(
                row.pageId() != null || row.externalUrl() != null ? new LytRect(
                    rowLayout.bounds()
                        .x(),
                    contentTop,
                    Math.max(
                        LIST_MARKER_SIZE + LIST_MARKER_GAP,
                        Math.min(
                            rowLayout.bounds()
                                .width(),
                            clickableWidth)),
                    Math.max(ICON_SIZE, clickableHeight)) : LytRect.empty());
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
            RenderRow row = rowLayout.row();
            if (row == null) {
                return false;
            }
            if (row.pageId() != null) {
                String rowAnchor = row.lineNumber() != null ? "line-" + row.lineNumber() : null;
                screen.navigateTo(new PageAnchor(row.pageId(), rowAnchor));
                return true;
            }
            if (row.loadMoreRow()) {
                loadMore();
                return true;
            }
            URI externalUri = MediaWikiExternalLinkSupport.resolveExternalUri(row.externalUrl());
            if (externalUri != null) {
                screen.openExternalUrl(externalUri);
                return true;
            }
            return false;
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
        hoveredRow = findTooltipRow((int) x, (int) y);
        return hoveredRow != null ? Optional.ofNullable(hoveredRow.tooltip()) : Optional.empty();
    }

    private int resolveColumnCount(MediaWikiSpecialPageResult visibleResult) {
        if (visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX && visibleResult.definition() != null
            && MediaWikiSpecialPageIds.SPECIAL_PAGES.equals(
                visibleResult.definition()
                    .name())) {
            return SPECIAL_PAGES_GROUP_COLUMNS;
        }
        if (visibleResult.definition() != null && (MediaWikiSpecialPageIds.ALL_TRANSLATIONS.equals(
            visibleResult.definition()
                .name())
            || MediaWikiSpecialPageIds.PAGE_TRANSLATION.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.GLOBAL_FILE_USAGE.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.DOUBLE_REDIRECTS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.LINT_ERRORS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.OVERRIDDEN_PAGES.equals(
                visibleResult.definition()
                    .name()))) {
            return 1;
        }
        if (visibleResult.definition() != null && MediaWikiSpecialPageIds.CATEGORY_TREE.equals(
            visibleResult.definition()
                .name())) {
            return 2;
        }
        return Math.max(1, rows);
    }

    private boolean isEmpty(MediaWikiSpecialPageResult visibleResult) {
        if (visibleResult == null) {
            return true;
        }
        return visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
            || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX
                ? visibleResult.groupedEntries()
                    .isEmpty()
                : visibleResult.flatEntries()
                    .isEmpty();
    }

    private List<List<GroupLayout>> layoutColumns(MediaWikiSpecialPageResult visibleResult) {
        int columnCount = resolveColumnCount(visibleResult);
        if (visibleResult.kind() != MediaWikiSpecialPageKind.GROUPED
            && visibleResult.kind() != MediaWikiSpecialPageKind.GROUP_INDEX) {
            return layoutFlatColumns(visibleResult, columnCount);
        }
        ArrayList<List<GroupLayout>> columns = new ArrayList<>(columnCount);
        ArrayList<Integer> heights = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            columns.add(new ArrayList<>());
            heights.add(0);
        }

        for (GroupLayout group : buildGroups(visibleResult)) {
            int targetColumn = 0;
            for (int index = 1; index < columnCount; index++) {
                if (heights.get(index) < heights.get(targetColumn)) {
                    targetColumn = index;
                }
            }
            columns.get(targetColumn)
                .add(group);
            heights.set(targetColumn, heights.get(targetColumn) + estimateHeight(group));
        }

        return columns;
    }

    private List<List<GroupLayout>> layoutFlatColumns(MediaWikiSpecialPageResult visibleResult, int columnCount) {
        ArrayList<List<GroupLayout>> columns = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            columns.add(new ArrayList<>());
        }
        List<MediaWikiSpecialListEntry> entries = visibleResult.flatEntries() != null ? visibleResult.flatEntries()
            : List.of();
        if (entries.isEmpty()) {
            columns.getFirst()
                .add(new GroupLayout(null, List.of()));
            return columns;
        }
        int perColumn = Math.max(1, (entries.size() + columnCount - 1) / columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int startIndex = columnIndex * perColumn;
            if (startIndex >= entries.size()) {
                break;
            }
            int endIndex = Math.min(entries.size(), startIndex + perColumn);
            columns.get(columnIndex)
                .add(new GroupLayout(null, new ArrayList<>(entries.subList(startIndex, endIndex))));
        }
        return columns;
    }

    private List<GroupLayout> buildGroups(MediaWikiSpecialPageResult visibleResult) {
        ArrayList<GroupLayout> groups = new ArrayList<>();
        if (visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
            || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            for (MediaWikiSpecialGroupedEntry group : visibleResult.groupedEntries()) {
                groups.add(new GroupLayout(group.title(), group.children() != null ? group.children() : List.of()));
            }
            return groups;
        }
        groups
            .add(new GroupLayout(null, visibleResult.flatEntries() != null ? visibleResult.flatEntries() : List.of()));
        return groups;
    }

    private MediaWikiSpecialPageResult applyVisibility(MediaWikiSpecialPageResult source, String rawQuery) {
        if (source == null) {
            return result;
        }
        String normalizedQuery = MediaWikiSpecialSearchSupport.normalize(rawQuery);
        VisibilityCache cached = visibilityCache;
        if (cached != null && cached.matches(source, normalizedQuery, currentVisibleCount)) {
            return cached.result();
        }
        MediaWikiSpecialPageResult computed = computeVisibility(source, normalizedQuery);
        visibilityCache = new VisibilityCache(source, normalizedQuery, currentVisibleCount, computed);
        return computed;
    }

    private MediaWikiSpecialPageResult computeVisibility(MediaWikiSpecialPageResult source, String normalizedQuery) {
        MediaWikiSpecialPageResult filtered = source;
        if (!normalizedQuery.isEmpty()) {
            if (source.kind() == MediaWikiSpecialPageKind.GROUPED
                || source.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
                filtered = new MediaWikiSpecialPageResult(
                    source.definition(),
                    source.kind(),
                    source.flatEntries(),
                    MediaWikiSpecialSearchSupport.filterGroupedEntries(source.groupedEntries(), normalizedQuery),
                    false,
                    source.searchEnabled());
            } else {
                filtered = new MediaWikiSpecialPageResult(
                    source.definition(),
                    source.kind(),
                    MediaWikiSpecialSearchSupport.filterFlatEntries(source.flatEntries(), normalizedQuery),
                    source.groupedEntries(),
                    false,
                    source.searchEnabled());
            }
            return filtered;
        }

        if (currentVisibleCount == Integer.MAX_VALUE) {
            return source;
        }

        if (source.kind() == MediaWikiSpecialPageKind.GROUPED
            || source.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            int totalChildren = countGroupedChildren(source.groupedEntries());
            return new MediaWikiSpecialPageResult(
                source.definition(),
                source.kind(),
                source.flatEntries(),
                MediaWikiSpecialSearchSupport.limit(source.groupedEntries(), currentVisibleCount),
                currentVisibleCount < totalChildren,
                source.searchEnabled());
        }
        return new MediaWikiSpecialPageResult(
            source.definition(),
            source.kind(),
            MediaWikiSpecialSearchSupport.limit(source.flatEntries(), currentVisibleCount),
            source.groupedEntries(),
            MediaWikiSpecialSearchSupport.hasMore(source.flatEntries(), currentVisibleCount),
            source.searchEnabled());
    }

    private int resolveDefaultVisibleCount(MediaWikiSpecialPageResult source) {
        if (source == null || source.definition() == null) {
            return Integer.MAX_VALUE;
        }
        return source.definition()
            .showsAllByDefault() ? Integer.MAX_VALUE
                : Math.max(
                    1,
                    source.definition()
                        .defaultVisibleCount());
    }

    private int countGroupedChildren(List<MediaWikiSpecialGroupedEntry> groups) {
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (MediaWikiSpecialGroupedEntry group : groups) {
            if (group != null && group.children() != null) {
                total += group.children()
                    .size();
            }
        }
        return total;
    }

    private void loadMore() {
        if (currentVisibleCount == Integer.MAX_VALUE) {
            return;
        }
        currentVisibleCount = Math.min(
            Integer.MAX_VALUE - MediaWikiSpecialPageQuery.PAGE_SIZE,
            currentVisibleCount + MediaWikiSpecialPageQuery.PAGE_SIZE);
        visibilityCache = null;
        maxPrecomputedContentHeight = 0;
        collectedFontFacts = null;
        var document = getDocument();
        if (document != null) {
            document.invalidateLayout();
        }
    }

    private int estimateHeight(GroupLayout group) {
        int height = group.title() != null ? HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM : 0;
        for (MediaWikiSpecialListEntry entry : group.entries()) {
            height += estimateEntryHeight(entry);
            height += ENTRY_GAP;
        }
        return height + GROUP_MARGIN;
    }

    @Nullable
    private RowLayout findClickableRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.row() != null && rowLayout.clickableBounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return null;
    }

    @Nullable
    private RowLayout findTooltipRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.tooltip() == null) {
                continue;
            }
            if (rowLayout.bounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return findClickableRow(x, y);
    }

    private int verticalCenterOffset(ResolvedTextStyle style, int boxHeight) {
        return Math.max(0, (boxHeight - GuideText.lineHeight(style)) / 2);
    }

    private int rowContentTop(ResolvedTextStyle style, List<String> subtitleLines) {
        if (subtitleLines.isEmpty()) {
            return 0;
        }
        return ENTRY_VERTICAL_PADDING_TOP;
    }

    private int rowContentHeight(ResolvedTextStyle style, List<String> subtitleLines) {
        if (subtitleLines.isEmpty()) {
            return Math.max(GuideText.lineHeight(style), ICON_SIZE);
        }
        return Math.max(
            ICON_SIZE,
            GuideText.lineHeight(style) + TITLE_SUBTITLE_GAP
                + GuideText.lineHeight(SUBTITLE_STYLE) * subtitleLines.size());
    }

    private int computeEntryHeight(LayoutContext context, MediaWikiSpecialListEntry entry, int columnWidth) {
        int textMaxWidth = computeTextMaxWidth(columnWidth, entry.icon() != null);
        List<String> subtitleLines = wrapLines(context, entry.subtitle(), textMaxWidth, SUBTITLE_STYLE);
        if (subtitleLines.isEmpty()) {
            return ENTRY_HEIGHT;
        }
        int contentHeight = Math.max(
            ICON_SIZE,
            context.getLineHeight(LINK_STYLE) + TITLE_SUBTITLE_GAP
                + context.getLineHeight(SUBTITLE_STYLE) * subtitleLines.size());
        return Math.max(ENTRY_HEIGHT, ENTRY_VERTICAL_PADDING_TOP + contentHeight + ENTRY_VERTICAL_PADDING_BOTTOM);
    }

    private int estimateEntryHeight(MediaWikiSpecialListEntry entry) {
        if (entry.subtitle() == null || entry.subtitle()
            .isEmpty()) {
            return ENTRY_HEIGHT;
        }
        int lineCount = Math.max(
            1,
            GuideStringLines.splitLines(entry.subtitle())
                .size());
        int lineHeight = GuideText.lineHeight(SUBTITLE_STYLE);
        return Math.max(
            ENTRY_HEIGHT,
            ENTRY_VERTICAL_PADDING_TOP + Math.max(ICON_SIZE, lineHeight + TITLE_SUBTITLE_GAP + lineHeight * lineCount)
                + ENTRY_VERTICAL_PADDING_BOTTOM);
    }

    private int computeTextMaxWidth(int columnWidth, boolean hasIcon) {
        return Math.max(1, columnWidth - LIST_MARKER_SIZE - LIST_MARKER_GAP - (hasIcon ? ICON_SIZE + ICON_GAP : 0));
    }

    private List<String> wrapLines(LayoutContext context, @Nullable String text, int maxWidth,
        ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        ArrayList<String> lines = new ArrayList<>();
        for (String rawLine : GuideStringLines.splitLines(text)) {
            appendWrappedLine(context, rawLine, maxWidth, style, lines);
        }
        return lines;
    }

    private List<String> wrapLines(@Nullable String text, int maxWidth, ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        ArrayList<String> lines = new ArrayList<>();
        for (String rawLine : GuideStringLines.splitLines(text)) {
            appendWrappedLine(rawLine, maxWidth, style, lines);
        }
        return lines;
    }

    private void appendWrappedLine(LayoutContext context, String rawLine, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        String line = rawLine != null ? rawLine.trim() : "";
        if (line.isEmpty()) {
            return;
        }
        if (measureTextWidth(context, style, line) <= maxWidth) {
            output.add(line);
            return;
        }
        String[] words = line.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measureTextWidth(context, style, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                output.add(current.toString());
                current.setLength(0);
            }
            appendBrokenWord(context, word, maxWidth, style, output);
        }
        if (!current.isEmpty()) {
            output.add(current.toString());
        }
    }

    private void appendWrappedLine(String rawLine, int maxWidth, ResolvedTextStyle style, List<String> output) {
        String line = rawLine != null ? rawLine.trim() : "";
        if (line.isEmpty()) {
            return;
        }
        if (GuideText.measureWidth(line, style) <= maxWidth) {
            output.add(line);
            return;
        }
        String[] words = line.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (GuideText.measureWidth(candidate, style) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                output.add(current.toString());
                current.setLength(0);
            }
            appendBrokenWord(word, maxWidth, style, output);
        }
        if (!current.isEmpty()) {
            output.add(current.toString());
        }
    }

    private void appendBrokenWord(LayoutContext context, String word, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        if (measureTextWidth(context, style, word) <= maxWidth) {
            output.add(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = start + 1;
            while (end <= word.length() && measureTextWidth(context, style, word.substring(start, end)) <= maxWidth) {
                end++;
            }
            int safeEnd = Math.max(start + 1, end - 1);
            output.add(word.substring(start, safeEnd));
            start = safeEnd;
        }
    }

    private void appendBrokenWord(String word, int maxWidth, ResolvedTextStyle style, List<String> output) {
        if (GuideText.measureWidth(word, style) <= maxWidth) {
            output.add(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = start + 1;
            while (end <= word.length() && GuideText.measureWidth(word.substring(start, end), style) <= maxWidth) {
                end++;
            }
            int safeEnd = Math.max(start + 1, end - 1);
            output.add(word.substring(start, safeEnd));
            start = safeEnd;
        }
    }

    private boolean areLinesClipped(List<String> renderedLines, String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return false;
        }
        List<String> originalLines = GuideStringLines.splitLines(originalText);
        if (renderedLines.size() != originalLines.size()) {
            return true;
        }
        for (int index = 0; index < renderedLines.size(); index++) {
            if (!renderedLines.get(index)
                .equals(originalLines.get(index))) {
                return true;
            }
        }
        return false;
    }

    private int measureTextWidth(LayoutContext context, ResolvedTextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            width += context.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
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

    private String clipToWidth(String text, int maxWidth, ResolvedTextStyle style) {
        if (text == null || text.isEmpty() || GuideText.measureWidth(text, style) <= maxWidth) {
            return text == null ? "" : text;
        }

        int ellipsisWidth = GuideText.measureWidth(ELLIPSIS, style);
        if (ellipsisWidth >= maxWidth) {
            return ELLIPSIS;
        }

        int end = text.length();
        while (end > 0 && GuideText.measureWidth(text.substring(0, end), style) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ELLIPSIS : text.substring(0, end) + ELLIPSIS;
    }

    private boolean isClipped(String rendered, String original) {
        if (original == null || original.isEmpty()) {
            return false;
        }
        return rendered != null && rendered.endsWith("...") && !rendered.equals(original);
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

    @Desugar
    private record GroupLayout(@Nullable String title, List<MediaWikiSpecialListEntry> entries) {}

    @Desugar
    private record RenderRow(String title, String subtitle, @Nullable ResourceLocation pageId,
        @Nullable Integer lineNumber, @Nullable GuidePageIcon icon, @Nullable String externalUrl, boolean header,
        boolean loadMoreRow) {

        public static RenderRow loadMore() {
            return new RenderRow("", "", null, null, null, null, false, true);
        }
    }

    @Desugar
    private record VisibilityCache(MediaWikiSpecialPageResult source, String normalizedQuery, int visibleCount,
        MediaWikiSpecialPageResult result) {

        private boolean matches(MediaWikiSpecialPageResult candidateSource, String candidateQuery,
            int candidateVisibleCount) {
            return source == candidateSource && normalizedQuery.equals(candidateQuery)
                && visibleCount == candidateVisibleCount;
        }
    }

    private static class RowLayout {

        private final LytRect bounds;
        @Nullable
        private final RenderRow row;
        private LytRect clickableBounds;
        @Nullable
        private GuideTooltip tooltip;

        private RowLayout(LytRect bounds, @Nullable RenderRow row) {
            this.bounds = bounds;
            this.row = row;
            this.clickableBounds = LytRect.empty();
            this.tooltip = null;
        }

        private LytRect bounds() {
            return bounds;
        }

        @Nullable
        private RenderRow row() {
            return row;
        }

        private LytRect clickableBounds() {
            return clickableBounds;
        }

        private void setClickableBounds(LytRect clickableBounds) {
            this.clickableBounds = clickableBounds != null ? clickableBounds : LytRect.empty();
        }

        private @Nullable GuideTooltip tooltip() {
            return tooltip;
        }

        private void setTooltip(@Nullable GuideTooltip tooltip) {
            this.tooltip = tooltip;
        }

        private RowLayout move(int deltaX, int deltaY) {
            RowLayout moved = new RowLayout(bounds.move(deltaX, deltaY), row);
            moved.setClickableBounds(clickableBounds.move(deltaX, deltaY));
            moved.setTooltip(tooltip);
            return moved;
        }
    }
}
