package com.hfstudio.guidenh.guide.internal.screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.internal.GuideBookmarkState;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

import lombok.Getter;

public class GuideNavBar {

    public static class NavigationTarget {

        @Nullable
        private final ResourceLocation guideId;
        @Nullable
        private final ResourceLocation pageId;

        public NavigationTarget(@Nullable ResourceLocation guideId, @Nullable ResourceLocation pageId) {
            this.guideId = guideId;
            this.pageId = pageId;
        }

        @Nullable
        public ResourceLocation guideId() {
            return guideId;
        }

        @Nullable
        public ResourceLocation pageId() {
            return pageId;
        }
    }

    public static class ContextTarget {

        @Nullable
        private final ResourceLocation guideId;
        @Nullable
        private final ResourceLocation pageId;

        public ContextTarget(@Nullable ResourceLocation guideId, @Nullable ResourceLocation pageId) {
            this.guideId = guideId;
            this.pageId = pageId;
        }

        @Nullable
        public ResourceLocation guideId() {
            return guideId;
        }

        @Nullable
        public ResourceLocation pageId() {
            return pageId;
        }
    }

    public static class ClickResult {

        @Nullable
        private final NavigationTarget navigationTarget;
        @Nullable
        private final ResourceLocation bookmarkTogglePageId;
        private final boolean pinToggle;
        private final boolean newPage;

        private ClickResult(@Nullable NavigationTarget navigationTarget,
            @Nullable ResourceLocation bookmarkTogglePageId, boolean pinToggle, boolean newPage) {
            this.navigationTarget = navigationTarget;
            this.bookmarkTogglePageId = bookmarkTogglePageId;
            this.pinToggle = pinToggle;
            this.newPage = newPage;
        }

        public static ClickResult navigate(@Nullable ResourceLocation guideId, @Nullable ResourceLocation pageId) {
            return pageId == null ? none() : new ClickResult(new NavigationTarget(guideId, pageId), null, false, false);
        }

        public static ClickResult toggleBookmark(ResourceLocation pageId) {
            return new ClickResult(null, pageId, false, false);
        }

        public static ClickResult togglePin() {
            return new ClickResult(null, null, true, false);
        }

        public static ClickResult createNewPage() {
            return new ClickResult(null, null, false, true);
        }

        public static ClickResult none() {
            return new ClickResult(null, null, false, false);
        }

        @Nullable
        public NavigationTarget navigationTarget() {
            return navigationTarget;
        }

        @Nullable
        public ResourceLocation bookmarkTogglePageId() {
            return bookmarkTogglePageId;
        }

        public boolean pinToggle() {
            return pinToggle;
        }

        public boolean shouldCreateNewPage() {
            return newPage;
        }
    }

    public static final int WIDTH_CLOSED = 10;
    public static final int WIDTH_OPEN = 150;
    public static final int CONTENT_PADDING = 2;
    public static final int TITLE_H = 16;
    public static final int ROW_H = 12;
    public static final int CHILD_INDENT = 3;
    public static final int EXPAND_INDENT = 8;
    public static final int ICON_SIZE = 9;
    public static final int ACTION_SLOT_W = 12;
    public static final int ACTION_ICON_SIZE = 9;
    public static final int ACTION_PADDING_RIGHT = 2;
    public static final int MIN_DYNAMIC_OPEN_WIDTH = 110;
    public static final int OPEN_WIDTH_SCREEN_PERCENT = 18;
    private static final int TITLE_TEXT_LEFT_PADDING = 6;
    private static final int TITLE_BUTTON_GAP = 1;
    private static final int TITLE_SCROLL_INTERVAL_MILLIS = 80;
    private static final String TITLE_SCROLL_GAP = "     ";

    // ---- GuideText (Rust) text styles --------------------------------------
    /**
     * Row text scale: 0.70 → lineHeight = round(17×0.70) = 12 == ROW_H, so the
     * line box exactly fills the row. Measured against msyh.ttc (see assumption
     * log): GuideText.ascent()=12.0 (scale 1) → row baseline = lineTop + 8.4;
     * CJK ink spans y∈[2.0, 9.0] within the 12px row — no poke-through, so no
     * downgrade to 0.65 was needed. Title text scale: 0.80 → lineHeight =
     * round(17×0.80) = 14 < TITLE_H 16; title baseline = lineTop + 9.6. No drop
     * shadow on either (mirrors legacy drawString(..., false)).
     */
    private static final float ROW_FONT_SCALE = 0.70f;
    private static final float TITLE_FONT_SCALE = 0.80f;
    private static final int TITLE_TEXT_COLOR = 0xFFE8E8E8;
    private static final ResolvedTextStyle ROW_TEXT_STYLE = navBarTextStyle(ROW_FONT_SCALE, 0xFFBBBBBB);
    private static final ResolvedTextStyle TITLE_TEXT_STYLE = navBarTextStyle(TITLE_FONT_SCALE, TITLE_TEXT_COLOR);

    private static ResolvedTextStyle navBarTextStyle(float fontScale, int argb) {
        return new ResolvedTextStyle(
            fontScale,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            null,
            new ConstantColor(argb),
            WhiteSpaceMode.NORMAL,
            TextAlignment.LEFT,
            false,
            null,
            false,
            0.0f);
    }

    /** Row style carrying the dynamic row tint (current/hover/normal/failed). */
    private static ResolvedTextStyle rowTextStyle(int color) {
        return navBarTextStyle(ROW_FONT_SCALE, color);
    }

    public interface GuideExpansionListener {

        void onExpansionToggled(@Nullable ResourceLocation guideId, ResourceLocation pageId, boolean expanded);
    }

    private final List<Row> rows = new ArrayList<>();
    private final Set<ResourceLocation> expandedPageIds = new HashSet<>();
    private final GuideNavProjection projection = new GuideNavProjection();
    private final StickyStack stickyStack = new StickyStack();
    @Nullable
    private NavigationTree lastTree;
    @Nullable
    private ResourceLocation activeGuideId;
    private int lastBookmarkStateVersion;
    private int lastExpandedStateHash;
    private boolean bookmarkGroupExpanded = true;
    @Nullable
    private GuideExpansionListener onExpansionToggled;

    private int x;
    private int y;
    private int height;
    private boolean open;
    @Getter
    private boolean pinned;
    private boolean contextMenuOpen;
    @Getter
    private int openWidth = WIDTH_OPEN;
    private int scrollY;
    private final SmoothFloatState visualScrollY = new SmoothFloatState();
    @Nullable
    private Row hoveredScrollingRow;
    private long hoveredScrollingStartedAtMillis;
    /** Lazily-created legacy RenderContext holder for the text PrimitiveCollector
     *  (only constructed once the Rust font system is available in-game). */
    @Nullable
    private VanillaRenderContext textRenderContext;

    public void setBounds(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    public void setOpenWidth(int openWidth) {
        this.openWidth = Math.max(WIDTH_CLOSED, openWidth);
    }

    public int currentWidth() {
        return (open || pinned) ? openWidth : WIDTH_CLOSED;
    }

    public boolean isOpen() {
        return open || pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        if (pinned) {
            this.open = true;
        }
    }

    public void setContextMenuOpen(boolean contextMenuOpen) {
        this.contextMenuOpen = contextMenuOpen;
        if (contextMenuOpen) {
            this.open = true;
        }
    }

    public void setOnExpansionToggled(@Nullable GuideExpansionListener listener) {
        this.onExpansionToggled = listener;
    }

    public void update(int mouseX, int mouseY, @Nullable NavigationTree tree, GuideBookmarkState bookmarkState) {
        if (shouldRebuildRows(tree, bookmarkState)) {
            rebuildRows(tree, bookmarkState);
        }
        if (pinned || contextMenuOpen) {
            open = true;
            return;
        }
        int w = currentWidth();
        open = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + height;
    }

    public GuideNavBarState captureState() {
        return GuideNavBarState.create(bookmarkGroupExpanded, new LinkedHashSet<>(expandedPageIds), scrollY);
    }

    public void restoreState(GuideNavBarState state, GuideBookmarkState bookmarkState) {
        GuideNavBarState effectiveState = state != null ? state : GuideNavBarState.defaultState();
        bookmarkGroupExpanded = effectiveState.bookmarkGroupExpanded();
        expandedPageIds.clear();
        expandedPageIds.addAll(
            effectiveState.expandedPageIds() != null ? effectiveState.expandedPageIds() : Collections.emptySet());
        lastExpandedStateHash = expandedPageIds.hashCode();
        scrollY = effectiveState.scrollY();
        visualScrollY.snapTo(scrollY);
        if (lastTree != null) {
            rebuildRows(lastTree, bookmarkState);
        }
    }

    /**
     * Switch the nav bar to a different guide's expansion state.
     * Replaces {@code expandedPageIds} with the saved state for the new guide,
     * then expands ancestors of {@code currentPageId}.
     * Caller is responsible for saving the old guide's state before calling this.
     */
    public void activateGuide(@Nullable ResourceLocation guideId, GuideNavBarState savedState,
        @Nullable NavigationTree tree, GuideBookmarkState bookmarkState, @Nullable ResourceLocation currentPageId,
        Set<ResourceLocation> carryOverIds) {

        // 1. Replace with new guide's saved state
        expandedPageIds.clear();
        if (savedState.expandedPageIds() != null) {
            expandedPageIds.addAll(savedState.expandedPageIds());
        }

        // 2. Merge carry-over IDs that exist in the new tree (captured before restoreViewState)
        if (carryOverIds != null && tree != null) {
            for (ResourceLocation id : carryOverIds) {
                if (tree.getNodeById(id) != null) {
                    expandedPageIds.add(id);
                }
            }
        }

        bookmarkGroupExpanded = savedState.bookmarkGroupExpanded();
        // Only restore scroll position during initial screen setup.
        // During cross-guide navigation, keep continuous scroll.
        if (tree == null) {
            scrollY = savedState.scrollY();
            visualScrollY.snapTo(scrollY);
        }
        activeGuideId = guideId;

        // 3. Expand ancestors of current page (inline, single rebuild at end)
        if (currentPageId != null && tree != null) {
            var path = tree.getPathTo(currentPageId);
            for (int i = 0; i < path.size() - 1; i++) {
                ResourceLocation parentId = path.get(i)
                    .pageId();
                if (parentId != null) {
                    expandedPageIds.add(parentId);
                }
            }
        }

        // 4. Single rebuild with final state
        if (tree != null) {
            rebuildRows(tree, bookmarkState);
        }
    }

    /** Snapshot of current expanded page IDs for carry-over before navigation. */
    public Set<ResourceLocation> getExpandedPageIdsSnapshot() {
        return new HashSet<>(expandedPageIds);
    }

    public void expandParentsTo(@Nullable NavigationTree tree, @Nullable ResourceLocation pageId,
        GuideBookmarkState bookmarkState) {
        if (tree == null || pageId == null) {
            return;
        }

        var path = tree.getPathTo(pageId);
        boolean changed = false;
        for (int index = 0; index < path.size() - 1; index++) {
            ResourceLocation parentPageId = path.get(index)
                .pageId();
            if (parentPageId != null) {
                changed |= expandedPageIds.add(parentPageId);
            }
        }
        if (changed) {
            rebuildRows(tree, bookmarkState);
        }
    }

    private boolean shouldRebuildRows(@Nullable NavigationTree tree, GuideBookmarkState bookmarkState) {
        return tree != lastTree || lastBookmarkStateVersion != bookmarkState.version()
            || lastExpandedStateHash != expandedPageIds.hashCode();
    }

    private void rebuildRows(@Nullable NavigationTree tree, GuideBookmarkState bookmarkState) {
        rows.clear();
        resetTitleScroll();
        lastTree = tree;
        if (tree == null) {
            lastBookmarkStateVersion = bookmarkState.version();
            lastExpandedStateHash = expandedPageIds.hashCode();
            return;
        }
        GuideNavProjection.ProjectionResult projected = projection
            .project(tree, bookmarkState, expandedPageIds, bookmarkGroupExpanded);
        for (GuideNavProjection.ProjectedRow projectedRow : projected.rows()) {
            rows.add(new Row(projectedRow));
        }
        lastBookmarkStateVersion = bookmarkState.version();
        lastExpandedStateHash = expandedPageIds.hashCode();
    }

    public void render(Minecraft mc, @Nullable ResourceLocation currentGuideId,
        @Nullable ResourceLocation currentPageId, int mouseX, int mouseY, @Nullable PageCollection pageCollection,
        GuideBookmarkState bookmarkState, boolean showNewPageButton) {
        updateVisualScroll();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            int w = currentWidth();
            int rowRight = x + w - 1;
            int textRightBase = x + w - 2;
            int bookmarkActionLeft = getBookmarkActionLeft(w);
            int bookmarkIconX = bookmarkActionLeft + ACTION_PADDING_RIGHT;
            int bgTop = 0xE0151515;
            int bgBot = 0xE0101010;
            drawVGradient(x, y, w, height, bgTop, bgBot);
            Gui.drawRect(rowRight, y, x + w, y + height, 0xFF2A2A2A);

            if (!isOpen()) {
                resetTitleScroll();
                drawArrow(x + w / 2 - 2, y + height / 2 - 3, true, 0xFF888888);
                return;
            }

            // Text pipeline: all bar text (title + rows) is collected as
            // GuideText glyph primitives and executed in one pass at the end,
            // after every legacy rect/icon draw (execute() restores GL state and
            // closes the scissor test, so it must be last). When the Rust font
            // system is not ready (init exception, handle=0) the collector stays
            // null and the legacy FontRenderer fallback keeps the bar legible.
            PrimitiveCollector textCollector = GuideText.isAvailable() ? newTextCollector(mc, w) : null;

            renderTitle(mc, textCollector, w, mouseX, mouseY, showNewPageButton);

            int bodyY = getBodyY();
            int bodyHeight = getBodyHeight();
            if (bodyHeight <= 0) {
                resetTitleScroll();
                executeNavText(textCollector);
                return;
            }

            int scaleFactor = DisplayScale.scaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            setBodyScissor(mc, scaleFactor);

            // Clip row text to the body via a screen-space scissor primitive: the
            // legacy GL scissor above clips only the legacy rects/icons, while
            // the engine owns text clipping inside the single execute pass.
            if (textCollector != null) {
                textCollector.pushScreenScissor(x, getBodyY(), currentWidth(), getBodyHeight());
            }

            StickyStack stickyRows = computeStickyStack(bodyY);
            int firstVisibleRow = getFirstVisibleRowIndex();
            int stickyPointer = 0;
            int nextStickyRowIndex = !stickyRows.isEmpty() ? stickyRows.rowIndexAt(0) : Integer.MAX_VALUE;
            boolean titleScrollActive = false;
            for (int rowIndex = firstVisibleRow; rowIndex < rows.size(); rowIndex++) {
                Row row = rows.get(rowIndex);
                int rowY = getRowY(rowIndex);
                if (rowY >= getBodyBottom()) {
                    break;
                }
                if (rowIndex == nextStickyRowIndex) {
                    stickyPointer++;
                    nextStickyRowIndex = stickyPointer < stickyRows.size() ? stickyRows.rowIndexAt(stickyPointer)
                        : Integer.MAX_VALUE;
                    continue;
                }
                titleScrollActive |= renderRow(
                    mc,
                    textCollector,
                    row,
                    rowY,
                    mouseX,
                    mouseY,
                    rowRight,
                    textRightBase,
                    currentGuideId,
                    currentPageId,
                    pageCollection,
                    bookmarkState,
                    bookmarkActionLeft,
                    bookmarkIconX,
                    scaleFactor,
                    false);
            }
            for (int stackIndex = 0; stackIndex < stickyRows.size(); stackIndex++) {
                titleScrollActive |= renderRow(
                    mc,
                    textCollector,
                    stickyRows.rowAt(stackIndex),
                    stickyRows.rowYAt(stackIndex),
                    mouseX,
                    mouseY,
                    rowRight,
                    textRightBase,
                    currentGuideId,
                    currentPageId,
                    pageCollection,
                    bookmarkState,
                    bookmarkActionLeft,
                    bookmarkIconX,
                    scaleFactor,
                    true);
            }
            if (!titleScrollActive) {
                resetTitleScroll();
            }

            if (textCollector != null) {
                textCollector.popScreenScissor();
            }
            executeNavText(textCollector);
        } finally {
            GL11.glPopAttrib();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private boolean renderRow(Minecraft mc, @Nullable PrimitiveCollector textCollector, Row row, int rowY, int mouseX,
        int mouseY, int rowRight, int textRightBase, @Nullable ResourceLocation currentGuideId,
        @Nullable ResourceLocation currentPageId, @Nullable PageCollection pageCollection,
        GuideBookmarkState bookmarkState, int bookmarkActionLeft, int bookmarkIconX, int scaleFactor, boolean sticky) {
        GuideNavProjection.DisplayRow displayRow = row.displayRow();
        int indent = row.indent();
        int rowX = x + 2 + indent;
        boolean hovered = mouseX >= x && mouseX < rowRight && mouseY >= rowY && mouseY < rowY + ROW_H;
        boolean current = isCurrentRow(row, currentGuideId, currentPageId);
        boolean bookmarkable = row.bookmarkable();

        if (sticky) {
            Gui.drawRect(x, rowY, rowRight, rowY + ROW_H, 0xF0181818);
            Gui.drawRect(x, rowY + ROW_H - 1, rowRight, rowY + ROW_H, 0x802A2A2A);
        }
        if (current) {
            Gui.drawRect(x, rowY, rowRight, rowY + ROW_H, 0x40FFFFFF);
        } else if (hovered) {
            Gui.drawRect(x, rowY, rowRight, rowY + ROW_H, 0x20FFFFFF);
        }

        if (row.hasChildren()) {
            boolean collapsed = isCollapsed(row);
            drawArrow(rowX, rowY + 2, collapsed, 0xFFCCCCCC);
        }

        int textX = rowX + EXPAND_INDENT;
        GuidePageIcon icon = displayRow.icon();
        if (icon != null) {
            int iconY = rowY + (ROW_H - ICON_SIZE) / 2;
            drawMiniIcon(mc, icon, textX, iconY);
            textX += ICON_SIZE + 2;
        }

        int textRight = textRightBase;
        if (bookmarkable) {
            textRight -= ACTION_SLOT_W;
        }
        int maxTw = textRight - textX;
        boolean titleScrollActive = false;
        if (maxTw > 0) {
            ResourceLocation pageId = row.pageId();
            boolean failed = pageId != null && pageCollection != null && pageCollection.isPageFailed(pageId);
            int color = getRowTextColor(current, hovered, failed);
            titleScrollActive = renderRowTitle(mc, textCollector, row, textX, rowY, maxTw, color, hovered, scaleFactor);
        }

        boolean bookmarkHovered = isInsideBookmarkAction(mouseX, mouseY, rowY, bookmarkable, bookmarkActionLeft);
        renderBookmarkIcon(mc, row, rowY, hovered, bookmarkHovered, bookmarkState, bookmarkIconX);
        return titleScrollActive;
    }

    private StickyStack computeStickyStack(int bodyY) {
        stickyStack.clear();
        if (rows.isEmpty()) {
            return stickyStack;
        }

        int firstVisibleRow = getFirstVisibleRowIndex();
        if (firstVisibleRow < 0 || firstVisibleRow >= rows.size()) {
            return stickyStack;
        }

        Row anchorRow = rows.get(firstVisibleRow);
        collectStickyAncestors(anchorRow);
        if (anchorRow.stickyCandidate() && getRowY(anchorRow.rowIndex()) < bodyY) {
            stickyStack.add(anchorRow);
        }
        if (stickyStack.isEmpty()) {
            return stickyStack;
        }

        int nextRowY = Integer.MAX_VALUE;
        for (int index = stickyStack.size() - 1; index >= 0; index--) {
            Row row = stickyStack.rowAt(index);
            int defaultRowY = bodyY + index * ROW_H;
            int subtreeBoundaryY = getRowY(row.subtreeEndRowIndexExclusive());
            int rowBottomLimit = Math.min(nextRowY, subtreeBoundaryY);
            int stickyRowY = Math.min(defaultRowY, rowBottomLimit - ROW_H);
            stickyStack.setRowYAt(index, stickyRowY);
            nextRowY = stickyRowY;
        }
        return stickyStack;
    }

    private void collectStickyAncestors(Row anchorRow) {
        collectStickyAncestorChain(anchorRow.parentRowIndex());
    }

    private void collectStickyAncestorChain(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        Row parentRow = rows.get(rowIndex);
        collectStickyAncestorChain(parentRow.parentRowIndex());
        if (parentRow.stickyCandidate()) {
            stickyStack.add(parentRow);
        }
    }

    private void resetTitleScroll() {
        hoveredScrollingRow = null;
        hoveredScrollingStartedAtMillis = 0L;
    }

    private void renderTitle(Minecraft mc, @Nullable PrimitiveCollector textCollector, int width, int mouseX,
        int mouseY, boolean showNewPageButton) {
        Gui.drawRect(x, y, x + width - 1, y + TITLE_H, 0xD0202020);
        Gui.drawRect(x, y + TITLE_H - 1, x + width - 1, y + TITLE_H, 0xFF2A2A2A);
        int pinX = getPinButtonX();
        int buttonY = getTitleButtonY();
        if (showNewPageButton) {
            int newPageX = getNewPageButtonX();
            boolean hovered = isInsideTitleButton(mouseX, mouseY, newPageX);
            int color = GuideIconButton.resolveIconColor(true, hovered, false);
            GuideIconButton.drawIcon(
                mc,
                GuideIconButton.Role.GUIDE_EDITOR_NEW_PAGE,
                newPageX,
                buttonY,
                GuideIconButton.WIDTH,
                GuideIconButton.HEIGHT,
                color);
        }

        boolean pinHovered = isInsideTitleButton(mouseX, mouseY, pinX);
        int pinColor = GuideIconButton.resolveIconColor(true, pinHovered, pinned);
        GuideIconButton.drawIcon(
            mc,
            GuideIconButton.Role.NAVIGATION_PIN,
            pinX,
            buttonY,
            GuideIconButton.WIDTH,
            GuideIconButton.HEIGHT,
            pinColor);

        int titleX = x + TITLE_TEXT_LEFT_PADDING;
        int titleRight = (showNewPageButton ? getNewPageButtonX() : pinX) - TITLE_BUTTON_GAP;
        int titleW = Math.max(0, titleRight - titleX);
        if (titleW > 0) {
            String title = GuidebookText.NavigationTitle.text();
            if (textCollector == null) {
                // Fallback: Rust font system not ready — keep the title legible
                // through the legacy FontRenderer.
                FontRenderer fr = mc.fontRenderer;
                String renderedTitle = fr.getStringWidth(title) > titleW
                    ? fr.trimStringToWidth(title, Math.max(0, titleW - 4)) + "…"
                    : title;
                fr.drawString(renderedTitle, titleX, y + (TITLE_H - fr.FONT_HEIGHT) / 2 + 1, TITLE_TEXT_COLOR, false);
            } else {
                String renderedTitle = GuideText.measureWidth(title, TITLE_TEXT_STYLE) > titleW
                    ? GuideText.clipToWidth(title, titleW, TITLE_TEXT_STYLE, GuideText.ClipSuffix.UNICODE_ELLIPSIS)
                    : title;
                GuideText.emitText(textCollector, renderedTitle, titleX, titleLineTop(), TITLE_TEXT_STYLE);
            }
        }
    }

    private boolean renderRowTitle(Minecraft mc, @Nullable PrimitiveCollector textCollector, Row row, int textX,
        int rowY, int maxTw, int color, boolean hovered, int scaleFactor) {
        if (textCollector == null) {
            return renderRowTitleFallback(mc, row, textX, rowY, maxTw, color, hovered, scaleFactor);
        }
        if (!hovered || row.getTitleWidth() <= maxTw) {
            GuideText.emitText(textCollector, row.getTitle(maxTw), textX, rowLineTop(rowY), rowTextStyle(color));
            return false;
        }

        int cycleWidth = row.getScrollCycleWidth();
        if (cycleWidth <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (hoveredScrollingRow != row) {
            hoveredScrollingRow = row;
            hoveredScrollingStartedAtMillis = now;
        }
        long elapsed = Math.max(0L, now - hoveredScrollingStartedAtMillis);
        int offset = (int) ((elapsed / TITLE_SCROLL_INTERVAL_MILLIS) % cycleWidth);
        int clipLeft = Math.max(textX, x);
        int clipRight = Math.min(textX + maxTw, x + currentWidth());
        int clipTop = Math.max(rowY, getBodyY());
        int clipBottom = Math.min(rowY + ROW_H, getBodyBottom());
        int clipWidth = clipRight - clipLeft;
        int clipHeight = clipBottom - clipTop;
        if (clipWidth <= 0 || clipHeight <= 0) {
            return false;
        }
        // Scroll clip as a screen-space scissor primitive (PrimitiveCollector
        // supports PushScreenScissor): the whole bar stays a single execute pass
        // with smooth scrolling — no per-row GL scissor churn.
        textCollector.pushScreenScissor(clipLeft, clipTop, clipWidth, clipHeight);
        GuideText.emitText(
            textCollector,
            row.getScrollingTitle(),
            textX - offset,
            rowLineTop(rowY),
            rowTextStyle(color));
        textCollector.popScreenScissor();
        return true;
    }

    /** Legacy FontRenderer path, used only while GuideText is unavailable. */
    private boolean renderRowTitleFallback(Minecraft mc, Row row, int textX, int rowY, int maxTw, int color,
        boolean hovered, int scaleFactor) {
        FontRenderer fr = mc.fontRenderer;
        String title = row.displayRow().title();
        if (!hovered || fr.getStringWidth(title) <= maxTw) {
            if (fr.getStringWidth(title) > maxTw) {
                title = fr.trimStringToWidth(title, Math.max(0, maxTw - 4)) + "…";
            }
            fr.drawString(title, textX, rowY + 2, color, false);
            return false;
        }

        int cycleWidth = fr.getStringWidth(title + TITLE_SCROLL_GAP);
        if (cycleWidth <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (hoveredScrollingRow != row) {
            hoveredScrollingRow = row;
            hoveredScrollingStartedAtMillis = now;
        }
        long elapsed = Math.max(0L, now - hoveredScrollingStartedAtMillis);
        int offset = (int) ((elapsed / TITLE_SCROLL_INTERVAL_MILLIS) % cycleWidth);
        int clipLeft = Math.max(textX, x);
        int clipRight = Math.min(textX + maxTw, x + currentWidth());
        int clipTop = Math.max(rowY, getBodyY());
        int clipBottom = Math.min(rowY + ROW_H, getBodyBottom());
        int clipWidth = clipRight - clipLeft;
        int clipHeight = clipBottom - clipTop;
        if (clipWidth <= 0 || clipHeight <= 0) {
            return false;
        }
        setScissor(mc, clipLeft, clipTop, clipWidth, clipHeight, scaleFactor);
        try {
            fr.drawString(row.getScrollingTitle(), textX - offset, rowY + 2, color, false);
        } finally {
            setBodyScissor(mc, scaleFactor);
        }
        return true;
    }

    @Nullable
    public ClickResult mouseClicked(int mouseX, int mouseY, @Nullable ResourceLocation currentGuideId,
        @Nullable ResourceLocation currentPageId, GuideBookmarkState bookmarkState, boolean showNewPageButton) {
        if (!isOpen()) {
            return null;
        }
        int w = currentWidth();
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + height) {
            return null;
        }
        if (mouseY < y + TITLE_H) {
            if (isInsideTitleButton(mouseX, mouseY, getPinButtonX())) {
                return ClickResult.togglePin();
            }
            if (showNewPageButton && isInsideTitleButton(mouseX, mouseY, getNewPageButtonX())) {
                return ClickResult.createNewPage();
            }
            return ClickResult.none();
        }
        StickyStack stickyStack = computeStickyStack(y + TITLE_H + CONTENT_PADDING);
        RowHit rowHit = pickRowAt(mouseY, stickyStack);
        if (rowHit == null) {
            return null;
        }

        Row row = rowHit.row();
        GuideNavProjection.DisplayRow displayRow = row.displayRow();
        int bookmarkActionLeft = getBookmarkActionLeft(w);
        if (row.bookmarkable() && isInsideBookmarkAction(mouseX, mouseY, rowHit.rowY(), true, bookmarkActionLeft)
            && row.pageId() != null) {
            return ClickResult.toggleBookmark(row.pageId());
        }

        if (row.hasChildren() && isInsideExpandArrow(mouseX, row)) {
            toggleExpand(row, bookmarkState);
            return ClickResult.none();
        }

        if (displayRow.kind() == GuideNavProjection.RowKind.BOOKMARK_GROUP) {
            bookmarkGroupExpanded = !bookmarkGroupExpanded;
            rebuildRows(lastTree, bookmarkState);
            return ClickResult.none();
        }

        if (row.hasChildren() && displayRow.kind() == GuideNavProjection.RowKind.TREE_PAGE) {
            boolean alreadyExpanded = isExpanded(row);
            if (!alreadyExpanded) {
                toggleExpand(row, bookmarkState);
            } else if (isCurrentRow(row, currentGuideId, currentPageId)) {
                toggleExpand(row, bookmarkState);
            }
        }

        if (row.pageId() != null && displayRow.hasPage()) {
            return ClickResult.navigate(row.guideId(), row.pageId());
        }
        return ClickResult.none();
    }

    @Nullable
    public ContextTarget getContextTarget(int mouseX, int mouseY) {
        if (!isOpen()) {
            return null;
        }
        int w = currentWidth();
        if (mouseX < x || mouseX >= x + w || mouseY < y + TITLE_H || mouseY >= y + height) {
            return null;
        }
        StickyStack stickyStack = computeStickyStack(y + TITLE_H + CONTENT_PADDING);
        RowHit rowHit = pickRowAt(mouseY, stickyStack);
        if (rowHit == null) {
            return null;
        }
        Row row = rowHit.row();
        GuideNavProjection.DisplayRow displayRow = row.displayRow();
        if (row.pageId() == null || !displayRow.hasPage()) {
            return null;
        }
        return new ContextTarget(row.guideId(), row.pageId());
    }

    @Nullable
    public String getTooltip(int mouseX, int mouseY, boolean showNewPageButton) {
        if (!isOpen() || !contains(mouseX, mouseY) || mouseY >= y + TITLE_H) {
            return null;
        }
        if (showNewPageButton && isInsideTitleButton(mouseX, mouseY, getNewPageButtonX())) {
            return GuideIconButton.Role.GUIDE_EDITOR_NEW_PAGE.tooltip();
        }
        if (isInsideTitleButton(mouseX, mouseY, getPinButtonX())) {
            return GuideIconButton.Role.NAVIGATION_PIN.tooltip();
        }
        return null;
    }

    private void renderBookmarkIcon(Minecraft mc, Row row, int rowY, boolean rowHovered, boolean buttonHovered,
        GuideBookmarkState bookmarkState, int bookmarkIconX) {
        if (!row.bookmarkable()) {
            return;
        }
        ResourceLocation pageId = row.pageId();
        boolean bookmarked = pageId != null && bookmarkState.isBookmarked(pageId);
        if (!bookmarked && !rowHovered) {
            return;
        }
        int iconY = getBookmarkIconY(rowY);
        GuideIconButton.Role role = bookmarked ? GuideIconButton.Role.BOOKMARKED : GuideIconButton.Role.BOOKMARK;
        int color = GuideIconButton.resolveIconColor(true, buttonHovered, bookmarked);
        GuideIconButton.drawIcon(mc, role, bookmarkIconX, iconY, ACTION_ICON_SIZE, ACTION_ICON_SIZE, color);
    }

    private boolean isInsideBookmarkAction(int mouseX, int mouseY, int rowY, boolean bookmarkable,
        int bookmarkActionLeft) {
        if (!bookmarkable) {
            return false;
        }
        return mouseX >= bookmarkActionLeft && mouseX < bookmarkActionLeft + Math.max(1, ACTION_SLOT_W - 1)
            && mouseY >= rowY
            && mouseY < rowY + ROW_H;
    }

    private int getBookmarkActionLeft(int width) {
        return x + width - ACTION_SLOT_W;
    }

    private int getBookmarkIconY(int rowY) {
        return rowY + (ROW_H - ACTION_ICON_SIZE) / 2;
    }

    private boolean isInsideExpandArrow(int mouseX, Row row) {
        int arrowX = x + 2 + row.indent();
        return mouseX >= arrowX && mouseX < arrowX + EXPAND_INDENT;
    }

    private boolean isCurrentRow(Row row, @Nullable ResourceLocation currentGuideId,
        @Nullable ResourceLocation currentPageId) {
        ResourceLocation pageId = row.pageId();
        if (currentPageId == null || !currentPageId.equals(pageId)) {
            return false;
        }
        return row.guideId() == null || currentGuideId == null || currentGuideId.equals(row.guideId());
    }

    private boolean isCollapsed(Row row) {
        if (row.kind() == GuideNavProjection.RowKind.BOOKMARK_GROUP) {
            return !bookmarkGroupExpanded;
        }
        return !isExpanded(row);
    }

    private boolean isExpanded(Row row) {
        return row.pageId() != null && expandedPageIds.contains(row.pageId());
    }

    private void toggleExpand(Row row, GuideBookmarkState bookmarkState) {
        if (row.kind() == GuideNavProjection.RowKind.BOOKMARK_GROUP) {
            bookmarkGroupExpanded = !bookmarkGroupExpanded;
        } else if (row.pageId() != null) {
            if (expandedPageIds.contains(row.pageId())) {
                expandedPageIds.remove(row.pageId());
            } else {
                expandedPageIds.add(row.pageId());
            }
        }
        rebuildRows(lastTree, bookmarkState);
        if (onExpansionToggled != null && row.pageId() != null) {
            onExpansionToggled.onExpansionToggled(row.guideId(), row.pageId(), expandedPageIds.contains(row.pageId()));
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        int w = currentWidth();
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + height;
    }

    public void scroll(int dwheel) {
        int contentH = rows.size() * ROW_H + CONTENT_PADDING * 2;
        int max = Math.max(0, contentH - Math.max(0, height - TITLE_H));
        scrollY -= Integer.signum(dwheel) * ROW_H * 2;
        if (scrollY < 0) {
            scrollY = 0;
        }
        if (scrollY > max) {
            scrollY = max;
        }
    }

    private int getFirstVisibleRowIndex() {
        int firstVisibleRow = Math.floorDiv(visualScrollY.rounded() - CONTENT_PADDING - 1, ROW_H);
        return Math.clamp(firstVisibleRow, 0, rows.size());
    }

    @Nullable
    private RowHit pickRowAt(int mouseY, StickyStack stickyStack) {
        for (int index = 0; index < stickyStack.size(); index++) {
            int rowY = stickyStack.rowYAt(index);
            if (mouseY >= rowY && mouseY < rowY + ROW_H) {
                return new RowHit(stickyStack.rowAt(index), rowY);
            }
        }

        int relativeY = mouseY - y - TITLE_H + visualScrollY.rounded() - CONTENT_PADDING;
        if (relativeY < 0) {
            return null;
        }
        int rowIndex = relativeY / ROW_H;
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        if (stickyStack.containsRowIndex(rowIndex)) {
            return null;
        }
        return new RowHit(rows.get(rowIndex), getRowY(rowIndex));
    }

    private int getRowY(int rowIndex) {
        return y + TITLE_H + CONTENT_PADDING - visualScrollY.rounded() + rowIndex * ROW_H;
    }

    private int getTitleButtonY() {
        return y + Math.max(0, (TITLE_H - GuideIconButton.HEIGHT) / 2);
    }

    private int getPinButtonX() {
        return x + currentWidth() - GuideIconButton.WIDTH - 1;
    }

    private int getNewPageButtonX() {
        return getPinButtonX() - GuideIconButton.WIDTH - TITLE_BUTTON_GAP;
    }

    private int getBodyY() {
        return y + TITLE_H;
    }

    private int getBodyHeight() {
        return Math.max(0, height - TITLE_H);
    }

    private void updateVisualScroll() {
        visualScrollY.updateTowards(scrollY, 28f, 0.25f, 0.01f, Math.max(128f, getBodyHeight() * 2f));
    }

    private int getBodyBottom() {
        return getBodyY() + getBodyHeight();
    }

    private boolean isInsideTitleButton(int mouseX, int mouseY, int buttonX) {
        int buttonY = getTitleButtonY();
        return mouseX >= buttonX && mouseX < buttonX + GuideIconButton.WIDTH
            && mouseY >= buttonY
            && mouseY < buttonY + GuideIconButton.HEIGHT;
    }

    private void setBodyScissor(Minecraft mc, int scaleFactor) {
        setScissor(mc, x, getBodyY(), currentWidth(), getBodyHeight(), scaleFactor);
    }

    private static void setScissor(Minecraft mc, int x, int y, int w, int h, int scaleFactor) {
        GL11.glScissor(x * scaleFactor, mc.displayHeight - (y + h) * scaleFactor, w * scaleFactor, h * scaleFactor);
    }

    // ---- GuideText primitive-pipeline helpers ------------------------------

    /**
     * Fresh per-frame text collector. The legacy RenderContext holder is created
     * once and reused (its scissor stack is never touched by the nav bar — text
     * clipping goes through screen-space scissor primitives instead).
     */
    private PrimitiveCollector newTextCollector(Minecraft mc, int width) {
        VanillaRenderContext ctx = textRenderContext;
        if (ctx == null) {
            ctx = new VanillaRenderContext(LightDarkMode.DARK_MODE, LytRect.empty(), mc.displayHeight);
            textRenderContext = ctx;
        }
        return new PrimitiveCollector(new LytRect(x, y, width, height), ctx);
    }

    /**
     * Execute the whole bar's collected text in one pass. Must run after every
     * legacy rect/icon draw: {@code GuideRenderEngine.execute} restores GL state
     * in its {@code finally} (including closing the GL scissor test), so text is
     * rendered last. The stale legacy body scissor is cleared first — inside
     * execute, clipping is owned by the PushScreenScissor primitives, which need
     * the engine's {@code guiScale} to match the current display scale, hence the
     * explicit beginFrame.
     */
    private static void executeNavText(@Nullable PrimitiveCollector textCollector) {
        if (textCollector == null) {
            return;
        }
        List<GuideRenderPrimitive> prims = textCollector.result();
        if (prims.isEmpty()) {
            return;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        var engine = LytDocument.getRenderEngine();
        engine.beginFrame(LytRect.empty(), DisplayScale.scaleFactor());
        engine.execute(prims);
    }

    // ---- headless chrome-pass collection (S2) ------------------------------

    /**
     * Headless chrome-pass collection: mirror of {@link #render} that emits the
     * whole bar — band background, title bar, row geometry and GuideText glyph
     * runs — as render primitives into {@code collector} instead of drawing to
     * the GL framebuffer. Text goes through the exact same S1 pipeline
     * (GuideText.emitText via {@link #renderRowTitle}); geometry that the live
     * path draws with Gui.drawRect / drawVGradient / drawArrow is emitted as
     * FillRect / GradientFill primitives. Texture-blit chrome (pin/new-page
     * buttons, page mini-icons, bookmark icons) is intentionally not emitted
     * (declared assumption in RenderPageService); row text x still advances
     * past the icon slot to match the live bar. The body clip uses a
     * document-space {@link PrimitiveCollector#pushScissor} (the headless
     * variant of the live PushScreenScissor) so the clip scales with the
     * offscreen render density exactly like the glyph runs.
     */
    public void collectPrimitives(@Nullable ResourceLocation currentGuideId,
        @Nullable ResourceLocation currentPageId, @Nullable PageCollection pageCollection,
        GuideBookmarkState bookmarkState, boolean showNewPageButton, PrimitiveCollector collector) {
        updateVisualScroll();
        int w = currentWidth();
        int rowRight = x + w - 1;
        int textRightBase = x + w - 2;
        int bookmarkActionLeft = getBookmarkActionLeft(w);
        int bookmarkIconX = bookmarkActionLeft + ACTION_PADDING_RIGHT;
        collector.emit(new GuideRenderPrimitive.GradientFill(x, y, w, height, 0xE0151515, 0xE0101010));
        collector.emit(new GuideRenderPrimitive.FillRect(rowRight, y, (x + w) - rowRight, height, 0xFF2A2A2A));

        if (!isOpen()) {
            emitArrowPrimitive(collector, x + w / 2 - 2, y + height / 2 - 3, true, 0xFF888888);
            return;
        }

        collector.emit(new GuideRenderPrimitive.FillRect(x, y, w - 1, TITLE_H, 0xD0202020));
        collector.emit(new GuideRenderPrimitive.FillRect(x, y + TITLE_H - 1, w - 1, 1, 0xFF2A2A2A));

        int titleX = x + TITLE_TEXT_LEFT_PADDING;
        int titleRight = (showNewPageButton ? getNewPageButtonX() : getPinButtonX()) - TITLE_BUTTON_GAP;
        int titleW = Math.max(0, titleRight - titleX);
        if (titleW > 0) {
            String title = GuidebookText.NavigationTitle.text();
            String renderedTitle = GuideText.measureWidth(title, TITLE_TEXT_STYLE) > titleW
                ? GuideText.clipToWidth(title, titleW, TITLE_TEXT_STYLE, GuideText.ClipSuffix.UNICODE_ELLIPSIS)
                : title;
            GuideText.emitText(collector, renderedTitle, titleX, titleLineTop(), TITLE_TEXT_STYLE);
        }

        int bodyY = getBodyY();
        int bodyHeight = getBodyHeight();
        if (bodyHeight <= 0) {
            return;
        }

        collector.pushScissor(x, bodyY, w, bodyHeight);

        StickyStack stickyRows = computeStickyStack(bodyY);
        int firstVisibleRow = getFirstVisibleRowIndex();
        int stickyPointer = 0;
        int nextStickyRowIndex = !stickyRows.isEmpty() ? stickyRows.rowIndexAt(0) : Integer.MAX_VALUE;
        for (int rowIndex = firstVisibleRow; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            int rowY = getRowY(rowIndex);
            if (rowY >= getBodyBottom()) {
                break;
            }
            if (rowIndex == nextStickyRowIndex) {
                stickyPointer++;
                nextStickyRowIndex = stickyPointer < stickyRows.size() ? stickyRows.rowIndexAt(stickyPointer)
                    : Integer.MAX_VALUE;
                continue;
            }
            collectRow(collector, row, rowY, rowRight, textRightBase, currentGuideId, currentPageId,
                pageCollection, bookmarkState, bookmarkActionLeft, bookmarkIconX, false);
        }
        for (int stackIndex = 0; stackIndex < stickyRows.size(); stackIndex++) {
            collectRow(collector, stickyRows.rowAt(stackIndex), stickyRows.rowYAt(stackIndex), rowRight,
                textRightBase, currentGuideId, currentPageId, pageCollection, bookmarkState, bookmarkActionLeft,
                bookmarkIconX, true);
        }
        collector.popScissor();
    }

    private void collectRow(PrimitiveCollector collector, Row row, int rowY, int rowRight, int textRightBase,
        @Nullable ResourceLocation currentGuideId, @Nullable ResourceLocation currentPageId,
        @Nullable PageCollection pageCollection, GuideBookmarkState bookmarkState, int bookmarkActionLeft,
        int bookmarkIconX, boolean sticky) {
        GuideNavProjection.DisplayRow displayRow = row.displayRow();
        int indent = row.indent();
        int rowX = x + 2 + indent;
        boolean current = isCurrentRow(row, currentGuideId, currentPageId);
        boolean bookmarkable = row.bookmarkable();

        GuideDebugLog.infoAlways(
            "[chrome] nav row y={} indent={} title={} kind={}",
            rowY, indent, displayRow.title(), displayRow.kind());

        if (sticky) {
            collector.emit(new GuideRenderPrimitive.FillRect(x, rowY, rowRight - x, ROW_H, 0xF0181818));
            collector.emit(new GuideRenderPrimitive.FillRect(x, rowY + ROW_H - 1, rowRight - x, 1, 0x802A2A2A));
        }
        if (current) {
            collector.emit(new GuideRenderPrimitive.FillRect(x, rowY, rowRight - x, ROW_H, 0x40FFFFFF));
        }

        if (row.hasChildren()) {
            emitArrowPrimitive(collector, rowX, rowY + 2, isCollapsed(row), 0xFFCCCCCC);
        }

        int textX = rowX + EXPAND_INDENT;
        GuidePageIcon icon = displayRow.icon();
        if (icon != null) {
            // Headless: texture/item mini-icon blits are skipped (GUI texture and
            // item rendering do not round-trip through the offscreen primitive
            // pipeline); the text x still advances past the icon slot to match
            // the live bar geometry.
            textX += ICON_SIZE + 2;
        }

        int textRight = textRightBase;
        if (bookmarkable) {
            textRight -= ACTION_SLOT_W;
        }
        int maxTw = textRight - textX;
        if (maxTw > 0) {
            ResourceLocation pageId = row.pageId();
            boolean failed = pageId != null && pageCollection != null && pageCollection.isPageFailed(pageId);
            int color = getRowTextColor(current, false, failed);
            renderRowTitle(Minecraft.getMinecraft(), collector, row, textX, rowY, maxTw, color, false,
                DisplayScale.scaleFactor());
        }
    }

    /** Emit the stepped expander/closed arrow as 1px quads (mirror of {@link #drawArrow}). */
    private static void emitArrowPrimitive(PrimitiveCollector collector, int x, int y, boolean pointRight,
        int color) {
        if (pointRight) {
            collector.emit(new GuideRenderPrimitive.FillRect(x, y, 1, 7, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 1, y + 1, 1, 5, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 2, y + 2, 1, 3, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 3, y + 3, 1, 1, color));
        } else {
            collector.emit(new GuideRenderPrimitive.FillRect(x, y, 7, 1, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 1, y + 1, 5, 1, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 2, y + 2, 3, 1, color));
            collector.emit(new GuideRenderPrimitive.FillRect(x + 3, y + 3, 1, 1, color));
        }
    }

    /** Line top that centers the row text's line box vertically in a ROW_H box. */
    private static int rowLineTop(int rowY) {
        return rowY + (ROW_H - GuideText.lineHeight(ROW_TEXT_STYLE)) / 2;
    }

    /** Line top that centers the title text's line box vertically in the TITLE_H bar. */
    private int titleLineTop() {
        return y + (TITLE_H - GuideText.lineHeight(TITLE_TEXT_STYLE)) / 2;
    }

    public static int getRowTextColor(boolean current, boolean hovered, boolean failed) {
        if (failed) {
            return current ? 0xFFFF9999 : hovered ? 0xFFFF7777 : 0xFFFF5555;
        }
        return current ? 0xFFFFFFFF : hovered ? 0xFF88BBFF : 0xFFBBBBBB;
    }

    public static void drawVGradient(int x, int y, int w, int h, int topColor, int botColor) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        var tes = Tessellator.instance;
        tes.startDrawingQuads();
        float ta = ((topColor >>> 24) & 0xFF) / 255f;
        float tr = ((topColor >>> 16) & 0xFF) / 255f;
        float tg = ((topColor >>> 8) & 0xFF) / 255f;
        float tb = (topColor & 0xFF) / 255f;
        float ba = ((botColor >>> 24) & 0xFF) / 255f;
        float br = ((botColor >>> 16) & 0xFF) / 255f;
        float bg = ((botColor >>> 8) & 0xFF) / 255f;
        float bb = (botColor & 0xFF) / 255f;
        tes.setColorRGBA_F(br, bg, bb, ba);
        tes.addVertex(x, y + h, 0);
        tes.addVertex(x + w, y + h, 0);
        tes.setColorRGBA_F(tr, tg, tb, ta);
        tes.addVertex(x + w, y, 0);
        tes.addVertex(x, y, 0);
        tes.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawArrow(int x, int y, boolean pointRight, int color) {
        if (pointRight) {
            Gui.drawRect(x, y, x + 1, y + 7, color);
            Gui.drawRect(x + 1, y + 1, x + 2, y + 6, color);
            Gui.drawRect(x + 2, y + 2, x + 3, y + 5, color);
            Gui.drawRect(x + 3, y + 3, x + 4, y + 4, color);
        } else {
            Gui.drawRect(x, y, x + 7, y + 1, color);
            Gui.drawRect(x + 1, y + 1, x + 6, y + 2, color);
            Gui.drawRect(x + 2, y + 2, x + 5, y + 3, color);
            Gui.drawRect(x + 3, y + 3, x + 4, y + 4, color);
        }
    }

    public static void drawMiniIcon(Minecraft mc, GuidePageIcon icon, int x, int y) {
        drawMiniIcon(mc, icon, x, y, ICON_SIZE);
    }

    public static void drawMiniIcon(Minecraft mc, GuidePageIcon icon, int x, int y, int size) {
        if (icon.isTextureIcon()) {
            drawMiniTextureIcon(icon.resolveCurrentTexture(), x, y, size);
            return;
        }
        drawMiniItemIcon(mc, icon.resolveCurrentItemStack(), x, y, size);
    }

    public static void drawMiniItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        drawMiniItemIcon(mc, stack, x, y, ICON_SIZE);
    }

    public static void drawMiniItemIcon(Minecraft mc, ItemStack stack, int x, int y, int size) {
        if (stack == null) {
            return;
        }
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0);
            float s = (float) size / 16f;
            GL11.glScalef(s, s, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderItem ri = RenderItem.getInstance();
            ri.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        } catch (Throwable ignored) {
            GL11.glPopMatrix();
        } finally {
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    public static void drawMiniTextureIcon(@Nullable GuidePageTexture texture, int x, int y) {
        drawMiniTextureIcon(texture, x, y, ICON_SIZE);
    }

    public static void drawMiniTextureIcon(@Nullable GuidePageTexture texture, int x, int y, int size) {
        if (texture == null || texture.isMissing()) {
            return;
        }

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture.getTexture());
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + size, 0, 0f, 1f);
        tess.addVertexWithUV(x + size, y + size, 0, 1f, 1f);
        tess.addVertexWithUV(x + size, y, 0, 1f, 0f);
        tess.addVertexWithUV(x, y, 0, 0f, 0f);
        tess.draw();
    }

    public static class Row {

        private final GuideNavProjection.ProjectedRow projectedRow;
        private final GuideNavProjection.DisplayRow displayRow;
        private final int rowIndex;
        private final int parentRowIndex;
        private final int subtreeEndRowIndexExclusive;
        private final int indent;
        private final boolean hasChildren;
        private final boolean bookmarkable;
        private final boolean stickyCandidate;
        @Nullable
        private final ResourceLocation guideId;
        @Nullable
        private final ResourceLocation pageId;
        @Nullable
        private String cachedTitle;
        @Nullable
        private String cachedScrollingTitle;
        private int cachedMaxTw = -1;
        private int cachedTitleWidth = -1;
        private int cachedScrollCycleWidth = -1;

        public Row(GuideNavProjection.ProjectedRow projectedRow) {
            this.projectedRow = projectedRow;
            displayRow = projectedRow.displayRow();
            rowIndex = projectedRow.rowIndex();
            parentRowIndex = projectedRow.parentRowIndex();
            subtreeEndRowIndexExclusive = projectedRow.subtreeEndRowIndexExclusive();
            indent = displayRow.depth() * CHILD_INDENT;
            hasChildren = displayRow.hasChildren();
            guideId = displayRow.guideId();
            pageId = displayRow.pageId();
            bookmarkable = displayRow.kind() == GuideNavProjection.RowKind.BOOKMARK_PAGE
                || displayRow.kind() == GuideNavProjection.RowKind.TREE_PAGE && displayRow.hasPage();
            stickyCandidate = displayRow.kind() == GuideNavProjection.RowKind.TREE_PAGE && hasChildren
                && subtreeEndRowIndexExclusive > rowIndex + 1;
        }

        public GuideNavProjection.ProjectedRow projectedRow() {
            return projectedRow;
        }

        public GuideNavProjection.DisplayRow displayRow() {
            return displayRow;
        }

        public int rowIndex() {
            return rowIndex;
        }

        public int parentRowIndex() {
            return parentRowIndex;
        }

        public int subtreeEndRowIndexExclusive() {
            return subtreeEndRowIndexExclusive;
        }

        public int indent() {
            return indent;
        }

        public boolean hasChildren() {
            return hasChildren;
        }

        public boolean bookmarkable() {
            return bookmarkable;
        }

        public boolean stickyCandidate() {
            return stickyCandidate;
        }

        @Nullable
        public ResourceLocation guideId() {
            return guideId;
        }

        @Nullable
        public ResourceLocation pageId() {
            return pageId;
        }

        public GuideNavProjection.RowKind kind() {
            return displayRow.kind();
        }

        public String getTitle(int maxTw) {
            if (maxTw == cachedMaxTw && cachedTitle != null) {
                return cachedTitle;
            }
            String title = displayRow.title();
            cachedTitle = getTitleWidth() > maxTw
                ? GuideText.clipToWidth(title, maxTw, ROW_TEXT_STYLE, GuideText.ClipSuffix.UNICODE_ELLIPSIS)
                : title;
            cachedMaxTw = maxTw;
            return cachedTitle;
        }

        public int getTitleWidth() {
            if (cachedTitleWidth < 0) {
                cachedTitleWidth = GuideText.measureWidth(displayRow.title(), ROW_TEXT_STYLE);
            }
            return cachedTitleWidth;
        }

        public int getScrollCycleWidth() {
            if (cachedScrollCycleWidth < 0) {
                cachedScrollCycleWidth = GuideText.measureWidth(displayRow.title() + TITLE_SCROLL_GAP, ROW_TEXT_STYLE);
            }
            return cachedScrollCycleWidth;
        }

        public String getScrollingTitle() {
            if (cachedScrollingTitle == null) {
                String title = displayRow.title();
                cachedScrollingTitle = title + TITLE_SCROLL_GAP + title;
            }
            return cachedScrollingTitle;
        }
    }

    private static class StickyStack {

        private final List<Row> rows = new ArrayList<>();
        private int[] rowYs = new int[0];
        private int[] rowIndices = new int[0];

        public void clear() {
            rows.clear();
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }

        public void add(Row row) {
            int index = rows.size();
            ensureCapacity(index + 1);
            rows.add(row);
            rowIndices[index] = row.rowIndex();
        }

        public void setRowYAt(int index, int rowY) {
            rowYs[index] = rowY;
        }

        public int size() {
            return rows.size();
        }

        public Row rowAt(int index) {
            return rows.get(index);
        }

        public int rowYAt(int index) {
            return rowYs[index];
        }

        public int rowIndexAt(int index) {
            return rowIndices[index];
        }

        public boolean containsRowIndex(int rowIndex) {
            for (int index = 0; index < rows.size(); index++) {
                if (rowIndices[index] == rowIndex) {
                    return true;
                }
            }
            return false;
        }

        private void ensureCapacity(int size) {
            if (rowYs.length >= size) {
                return;
            }
            int newSize = Math.max(size, Math.max(4, rowYs.length * 2));
            rowYs = Arrays.copyOf(rowYs, newSize);
            rowIndices = Arrays.copyOf(rowIndices, newSize);
        }
    }

    private static class RowHit {

        private final Row row;
        private final int rowY;

        private RowHit(Row row, int rowY) {
            this.row = row;
            this.rowY = rowY;
        }

        public Row row() {
            return row;
        }

        public int rowY() {
            return rowY;
        }
    }
}
