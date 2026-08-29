package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;

public class LytDetailsBlock extends LytBlock implements InteractiveElement, LytBlockContainer, DocumentDragTarget {

    private static final ConstantColor SUMMARY_COLOR = new ConstantColor(0xFFE2E6ED);
    private static final String SUMMARY_OPEN_MARKER = "v";
    private static final String SUMMARY_CLOSED_MARKER = ">";
    private static final String DEFAULT_SUMMARY_TEXT = "Details";
    private static final int PADDING = 6;
    private static final int GAP = 4;
    private static final int BORDER_WIDTH = 1;
    /**
     * Top margin on the summary-marker paragraph (in px) that optically centers
     * the ">"/"v" marker glyphs against the summary text. The marker glyphs are
     * mid-em symbols whose ink sits higher inside their 1.55x line-height box
     * than the text's visual (x-height) centerline; the row's alignItems CENTER
     * aligns whole line-height boxes, so the marker needs its own box nudged
     * down. Taffy's CENTER aligns the margin box, which shifts the marker's
     * content box down by half this margin (same declaration pattern as the
     * search-result icon rows, GuideSearchResultDocumentBuilder
     * RESULT_ICON_MARGIN_TOP). A 1px margin = 0.5px downward shift; measured
     * against the actual guide font (Microsoft YaHei) ">" sits ~0.55px high and
     * "v" ~0px off the x-height middle, so half a pixel puts ">" on the
     * centerline with only a negligible nudge to "v".
     */
    private static final int SUMMARY_MARKER_ALIGN_MARGIN_TOP = 1;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 4;
    private static final int MIN_SCROLLBAR_THUMB = 14;
    private static final int MIN_WHEEL_STEP = 16;
    private static final BorderStyle DETAILS_BORDER = new BorderStyle(SymbolicColor.TABLE_BORDER, BORDER_WIDTH);

    private final LytHBox summaryRow = new LytHBox();
    private final LytParagraph summaryMarker = new LytParagraph();
    private final LytParagraph summaryContent = new LytParagraph();
    /** Scrollable content viewport; clips its children to its own bounds. */
    private final LytViewportBox content = new LytViewportBox();
    private final BorderRenderer borderRenderer = new BorderRenderer();
    private final SmoothFloatState visualContentScrollOffsetY = new SmoothFloatState();

    @Getter
    private boolean open;
    @Nullable
    private String fallbackSummaryText;
    @Getter
    private int preferredWidth;
    @Getter
    private int preferredContentHeight;
    private int contentScrollOffsetY;
    /** Visual-scroll delta currently baked into the content bounds (see computePrimitives). */
    private int visualDeltaY;
    private boolean draggingContent;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;

    public LytDetailsBlock() {
        summaryRow.parent = this;
        summaryRow.setGap(4);
        summaryRow.setWrap(false);
        summaryRow.setFullWidth(true);
        summaryRow.setAlignItems(AlignItems.CENTER);

        summaryMarker.setMarginTop(SUMMARY_MARKER_ALIGN_MARGIN_TOP);
        summaryMarker.setMarginBottom(0);
        summaryMarker.modifyStyle(
            style -> style.bold(true)
                .color(SUMMARY_COLOR));

        summaryContent.setMarginTop(0);
        summaryContent.setMarginBottom(0);
        summaryContent.modifyStyle(
            style -> style.bold(true)
                .color(SUMMARY_COLOR));

        content.parent = this;
        content.setGap(4);
        // No setFullWidth(true) on purpose: fullWidth serializes as an explicit
        // align-self Stretch, which under Taffy 0.12 resolves against the viewport
        // width WITHOUT subtracting the declared margins — the viewport would
        // overflow the details body by 2*(PADDING+BORDER_WIDTH) on each side.
        // Leaving the cross size auto makes align-self Auto, so it inherits this
        // block's default alignItems Stretch (LayoutStyleExtractor returns Stretch
        // for every non-LytAxisBox container), and the stretch computes the width
        // as details body width minus the margins below.
        // Inset the content viewport inside the details body by the padding +
        // border (7px/side): Rust positions the viewport at details.x+7. Declared
        // as margins (LytDetailsBlock is a plain LytBlock — it has no box
        // padding/border to serialize; LayoutStyleExtractor carries block
        // margins to the FlatBuffer style verbatim).
        content.setMarginLeft(PADDING + BORDER_WIDTH);
        content.setMarginRight(PADDING + BORDER_WIDTH);

        summaryRow.append(summaryMarker);
        summaryRow.append(summaryContent);
        syncSummaryMarker();
        syncContentVisibility();
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
        setFullWidth(this.preferredWidth <= 0);
    }

    public void setPreferredContentHeight(int preferredContentHeight) {
        this.preferredContentHeight = Math.max(0, preferredContentHeight);
    }

    public LytParagraph getSummaryBox() {
        return summaryContent;
    }

    public void setFallbackSummaryText(@Nullable String fallbackSummaryText) {
        this.fallbackSummaryText = fallbackSummaryText;
        syncSummaryFallback();
    }

    public void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            syncSummaryMarker();
            syncContentVisibility();
            var document = getDocument();
            if (document != null) {
                document.invalidateLayout();
            }
        }
    }

    public LytVBox getContentBox() {
        return content;
    }

    private void syncSummaryMarker() {
        summaryMarker.clearContent();
        summaryMarker.appendText(open ? SUMMARY_OPEN_MARKER : SUMMARY_CLOSED_MARKER);
    }

    private void syncSummaryFallback() {
        if (!summaryContent.isEmpty()) {
            return;
        }
        summaryContent.clearContent();
        summaryContent.appendText(
            fallbackSummaryText != null && !fallbackSummaryText.trim()
                .isEmpty() ? fallbackSummaryText : DEFAULT_SUMMARY_TEXT);
    }

    private void syncContentVisibility() {
        syncSummaryFallback();
    }

    @Override
    public void append(LytBlock block) {
        content.append(block);
    }

    @Override
    public void removeChild(LytNode node) {
        content.removeChild(node);
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        content.replaceChild(oldChild, newChild);
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return open ? List.of(summaryRow, content) : List.of(summaryRow);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(availableWidth, preferredWidth))
            : Math.max(1, availableWidth);
        int innerX = x + PADDING + BORDER_WIDTH;
        int innerY = y + PADDING + BORDER_WIDTH;
        int innerWidth = Math.max(1, safeWidth - (PADDING + BORDER_WIDTH) * 2);

        LytRect summaryBounds = summaryRow.layout(context, innerX, innerY, innerWidth);
        int totalHeight = PADDING + BORDER_WIDTH + summaryBounds.height() + PADDING + BORDER_WIDTH;

        if (open) {
            int contentX = innerX;
            int contentY = summaryBounds.bottom() + GAP;
            int contentWidth = innerWidth;
            LytRect measuredContent = content.layout(context, contentX, contentY, contentWidth);
            int naturalHeight = measuredContent.height();
            int viewportHeight = preferredContentHeight > 0 ? preferredContentHeight : naturalHeight;
            if (preferredContentHeight > 0 && naturalHeight > viewportHeight) {
                contentWidth = Math.max(1, innerWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
                measuredContent = content.layout(context, contentX, contentY, contentWidth);
                naturalHeight = measuredContent.height();
                viewportHeight = preferredContentHeight;
            }
            content.setExplicitHeight(viewportHeight);
            setContentScrollOffset(contentScrollOffsetY);
            snapVisualScrollToTarget();
            totalHeight = PADDING + BORDER_WIDTH
                + summaryBounds.height()
                + GAP
                + viewportHeight
                + PADDING
                + BORDER_WIDTH;
        } else {
            content.setExplicitHeight(-1);
            setContentScrollOffset(0);
            snapVisualScrollToTarget();
        }

        return new LytRect(x, y, safeWidth, totalHeight);
    }

    // ---- derived geometry (computed from current bounds; no layout-time fields) ----

    /** The content viewport rect is simply the viewport box's own bounds. */
    private LytRect getContentViewportBounds() {
        return content.getBounds();
    }

    /**
     * Natural (unscrolled) content height, derived from the children's current
     * bounds. The scroll offset cancels out: children and the content box move
     * together.
     */
    private int getContentNaturalHeight() {
        int bottom = Integer.MIN_VALUE;
        for (LytNode child : content.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                bottom = Math.max(
                    bottom,
                    childBlock.getBounds()
                        .bottom());
            }
        }
        return bottom == Integer.MIN_VALUE ? 0
            : Math.max(
                0,
                bottom - content.getBounds()
                    .y());
    }

    private int getContentViewportHeight() {
        return preferredContentHeight > 0 ? preferredContentHeight : getContentNaturalHeight();
    }

    private int getMaxContentScroll() {
        return Math.max(0, getContentNaturalHeight() - getContentViewportHeight());
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        summaryRow.moveLayoutPos(deltaX, deltaY);
        content.moveLayoutPos(deltaX, deltaY);
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        c.emit(
            new GuideRenderPrimitive.FillRect(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                SymbolicColor.BLOCKQUOTE_BACKGROUND.resolve(LightDarkMode.current())));

        // Advance the smooth scroll and bake the visual delta into the content
        // bounds (collector traverses the content right after this).
        updateVisualScroll();
        int newDelta = contentScrollOffsetY - visualContentScrollOffsetY.rounded();
        if (open && newDelta != visualDeltaY
            && !content.getBounds()
                .isEmpty()) {
            content.moveLayoutPos(0, newDelta - visualDeltaY);
            visualDeltaY = newDelta;
        }

        LytRect trackBounds = getScrollbarTrackBounds();
        if (open && !trackBounds.isEmpty()) {
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    trackBounds.x(),
                    trackBounds.y(),
                    trackBounds.width(),
                    trackBounds.height(),
                    0x30242B33));
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty()) {
                c.emit(
                    new GuideRenderPrimitive.FillRect(
                        thumbBounds.x(),
                        thumbBounds.y(),
                        thumbBounds.width(),
                        thumbBounds.height(),
                        draggingScrollbar ? 0xFFCDD6E1 : 0xA0AAB5C2));
            }
        }
    }

    @Override
    public void emitDecorations(PrimitiveCollector c) {
        c.emit(
            new GuideRenderPrimitive.DrawBorder(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                BORDER_WIDTH,
                BORDER_WIDTH,
                BORDER_WIDTH,
                BORDER_WIDTH,
                DETAILS_BORDER.color() != null ? DETAILS_BORDER.color()
                    .resolve(LightDarkMode.current()) : 0xFF000000));
    }

    @Override
    protected void afterExternalLayout() {
        // The writeback reset the content to the unscrolled position; re-apply
        // the current scroll offset and restart the visual-delta bookkeeping.
        updateContentPosition();
        visualDeltaY = 0;
    }

    @Override
    public void render(RenderContext context) {}

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }

        LytRect summaryBounds = summaryRow.getBounds();
        if (summaryBounds != null && summaryBounds.contains(x, y)) {
            setOpen(!open);
            return true;
        }
        return false;
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!open || button != 0 || getMaxContentScroll() <= 0) {
            return false;
        }
        if (getScrollbarTrackBounds().contains(documentX, documentY)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(documentX, documentY)) {
                scrollbarGrabOffsetY = documentY - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(documentY);
            }
            draggingScrollbar = true;
            draggingContent = false;
            return true;
        }
        if (!getContentViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        draggingContent = true;
        draggingScrollbar = false;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (draggingScrollbar) {
            updateScrollFromMouseY(documentY);
            return;
        }
        if (!draggingContent) {
            return;
        }
        int deltaY = documentY - dragLastDocumentY;
        dragLastDocumentY = documentY;
        setContentScrollOffset(contentScrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingContent = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (!open || wheelDelta == 0
            || getMaxContentScroll() <= 0
            || !getContentViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        setContentScrollOffset(contentScrollOffsetY - Integer.signum(wheelDelta) * MIN_WHEEL_STEP);
        return true;
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        if (summaryRow.getBounds() != null && summaryRow.getBounds()
            .contains(x, y)) {
            LytNode node = summaryRow.pickNode(x, y);
            return node != null ? node : this;
        }
        if (open && getScrollbarTrackBounds().contains(x, y)) {
            return this;
        }
        if (open && getContentViewportBounds().contains(x, y)) {
            LytNode node = content.pickNode(x, y);
            return node != null ? node : this;
        }
        return this;
    }

    private void renderScrollbar(RenderContext context) {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            return;
        }
        context.fillRect(trackBounds, 0x30242B33);
        LytRect thumbBounds = getScrollbarThumbBounds();
        if (!thumbBounds.isEmpty()) {
            context.fillRect(thumbBounds, draggingScrollbar ? 0xFFCDD6E1 : 0xA0AAB5C2);
        }
    }

    private LytRect getScrollbarTrackBounds() {
        if (getMaxContentScroll() <= 0) {
            return LytRect.empty();
        }
        LytRect viewport = getContentViewportBounds();
        return new LytRect(viewport.right() + SCROLLBAR_GAP, viewport.y(), SCROLLBAR_WIDTH, viewport.height());
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return LytRect.empty();
        }
        int thumbHeight = Math.max(
            MIN_SCROLLBAR_THUMB,
            track.height() * track.height() / Math.max(track.height(), getContentNaturalHeight()));
        thumbHeight = Math.min(thumbHeight, track.height());
        int maxScroll = getMaxContentScroll();
        int thumbTrack = Math.max(1, track.height() - thumbHeight);
        int thumbY = track.y();
        if (maxScroll > 0) {
            thumbY += (int) ((long) thumbTrack * visualContentScrollOffsetY.rounded() / maxScroll);
        }
        return new LytRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private void setContentScrollOffset(int contentScrollOffsetY) {
        this.contentScrollOffsetY = SceneEditorVerticalScrollbar.clamp(contentScrollOffsetY, 0, getMaxContentScroll());
        updateContentPosition();
    }

    private void updateContentPosition() {
        if (!content.getBounds()
            .isEmpty()
            && !summaryRow.getBounds()
                .isEmpty()) {
            int naturalX = bounds.x() + PADDING + BORDER_WIDTH;
            int naturalY = summaryRow.getBounds()
                .bottom() + GAP;
            content.moveLayoutPos(
                naturalX - content.getBounds()
                    .x(),
                naturalY - contentScrollOffsetY
                    - content.getBounds()
                        .y());
        }
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect track = getScrollbarTrackBounds();
        LytRect thumb = getScrollbarThumbBounds();
        if (track.isEmpty() || thumb.isEmpty()) {
            setContentScrollOffset(0);
            return;
        }
        int thumbTrack = Math.max(1, track.height() - thumb.height());
        int thumbTop = SceneEditorVerticalScrollbar
            .clamp(mouseY - scrollbarGrabOffsetY, track.y(), track.y() + thumbTrack);
        int maxScroll = getMaxContentScroll();
        setContentScrollOffset((int) ((long) (thumbTop - track.y()) * maxScroll / thumbTrack));
    }

    private void snapVisualScrollToTarget() {
        visualContentScrollOffsetY.snapTo(contentScrollOffsetY);
    }

    private void updateVisualScroll() {
        visualContentScrollOffsetY
            .updateTowards(contentScrollOffsetY, 28f, 0.25f, 0.01f, Math.max(128f, getContentViewportHeight() * 2f));
    }
}
