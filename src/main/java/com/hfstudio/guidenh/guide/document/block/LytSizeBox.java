package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytSizeBox extends LytVBox implements DocumentDragTarget {

    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 4;
    private static final int MIN_WHEEL_STEP = 16;

    private final BorderRenderer borderRenderer = new BorderRenderer();

    @Getter
    private int preferredWidth;
    @Getter
    private int preferredHeight;
    /** Scrollable viewport wrapping the content; clips children to its bounds. */
    private final LytViewportBox viewport = new LytViewportBox();
    /** Content container: receives all externally appended children. */
    private final LytVBox content = new LytVBox();
    private int scrollOffsetY;
    private int appliedScrollOffsetY;
    /** Visual-scroll delta currently baked into the content bounds (see computePrimitives). */
    private int visualDeltaY;
    private final SmoothFloatState visualScrollOffsetY = new SmoothFloatState();
    private boolean draggingContent;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;

    public LytSizeBox() {
        viewport.setFullWidth(true);
        viewport.append(content);
        super.append(viewport);
    }

    /** External content is appended into the inner content box, inside the viewport. */
    @Override
    public void append(LytBlock block) {
        content.append(block);
    }

    @Override
    public void removeChild(LytNode node) {
        content.removeChild(node);
    }

    @Override
    public void clearContent() {
        content.clearContent();
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = Math.max(0, preferredHeight);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int constrainedWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        int measuredWidth = Math.max(1, constrainedWidth);
        int contentWidth = measuredWidth;

        LytRect contentBounds = content.layout(context, x, y, measuredWidth);
        int contentH = contentBounds.height();
        int viewportH = preferredHeight > 0 ? preferredHeight : contentH;
        if (preferredHeight > 0 && contentH > viewportH) {
            contentWidth = Math.max(1, measuredWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
            contentBounds = content.layout(context, x, y, contentWidth);
            contentH = contentBounds.height();
            viewportH = preferredHeight;
        }

        viewport.setExplicitHeight(viewportH);
        viewport.layout(context, x, y, contentWidth);
        setScrollOffset(scrollOffsetY);
        snapVisualScrollToTarget();

        int totalWidth = preferredWidth > 0 ? measuredWidth
            : contentBounds.width() + (hasVerticalScroll() ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        return new LytRect(x, y, totalWidth, viewportH);
    }

    // ---- derived geometry (computed from current bounds; no layout-time fields) ----

    private int getContentHeight() {
        return content.getBounds()
            .height();
    }

    private int getViewportHeight() {
        return preferredHeight > 0 ? preferredHeight : getContentHeight();
    }

    private LytRect getViewportBounds() {
        int x = bounds.x() + getBorderLeft().width() + paddingLeft;
        int y = bounds.y() + getBorderTop().width() + paddingTop;
        int w = bounds.right() - getBorderRight().width() - paddingRight - x;
        if (hasVerticalScroll()) {
            w = Math.max(1, w - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
        }
        return new LytRect(x, y, Math.max(0, w), Math.max(0, getViewportHeight()));
    }

    private int getMaxScrollOffset() {
        return Math.max(0, getContentHeight() - getViewportHeight());
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);

        // Advance the smooth scroll and bake the visual delta into the content
        // bounds (collector traverses the content right after this).
        updateVisualScroll();
        int newDelta = appliedScrollOffsetY - visualScrollOffsetY.rounded();
        if (newDelta != visualDeltaY && !content.getBounds()
            .isEmpty()) {
            content.moveLayoutPos(0, newDelta - visualDeltaY);
            visualDeltaY = newDelta;
        }

        LytRect trackBounds = getScrollbarTrackBounds();
        if (!trackBounds.isEmpty()) {
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
    protected void afterExternalLayout() {
        // The writeback reset the content to the unscrolled position; re-apply
        // the current scroll offset and restart the visual-delta bookkeeping.
        content.moveLayoutPos(0, appliedScrollOffsetY - scrollOffsetY);
        appliedScrollOffsetY = scrollOffsetY;
        visualDeltaY = 0;
    }

    @Override
    public void render(RenderContext context) {}

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!hasVerticalScroll() || button != 0) {
            return false;
        }

        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.contains(documentX, documentY)) {
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

        if (!getViewportBounds().contains(documentX, documentY)) {
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
        setScrollOffset(scrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingContent = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (wheelDelta == 0 || !hasVerticalScroll() || !getViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        setScrollOffset(scrollOffsetY - Integer.signum(wheelDelta) * MIN_WHEEL_STEP);
        return true;
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        if (getScrollbarTrackBounds().contains(x, y)) {
            return this;
        }
        if (!getViewportBounds().contains(x, y)) {
            return this;
        }

        for (LytNode child : getChildren()) {
            LytNode node = child.pickNode(x, y);
            if (node != null) {
                return node;
            }
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

    private void renderBorder(RenderContext context) {
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            borderRenderer
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
    }

    private void setScrollOffset(int scrollOffsetY) {
        this.scrollOffsetY = SceneEditorVerticalScrollbar.clamp(scrollOffsetY, 0, getMaxScrollOffset());
        int deltaY = appliedScrollOffsetY - this.scrollOffsetY;
        if (deltaY != 0) {
            content.moveLayoutPos(0, deltaY);
            appliedScrollOffsetY = this.scrollOffsetY;
        }
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            setScrollOffset(0);
            return;
        }

        setScrollOffset(
            SceneEditorVerticalScrollbar.offsetFromDrag(
                mouseY,
                scrollbarGrabOffsetY,
                trackBounds.y(),
                trackBounds.height(),
                getContentHeight(),
                getViewportHeight()));
    }

    private boolean hasVerticalScroll() {
        return getMaxScrollOffset() > 0;
    }

    private LytRect getScrollbarTrackBounds() {
        if (!hasVerticalScroll()) {
            return LytRect.empty();
        }
        LytRect viewportBounds = getViewportBounds();
        return new LytRect(
            viewportBounds.right() + SCROLLBAR_GAP,
            viewportBounds.y(),
            SCROLLBAR_WIDTH,
            viewportBounds.height());
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            return LytRect.empty();
        }

        SceneEditorVerticalScrollbar.Thumb thumb = SceneEditorVerticalScrollbar.computeThumb(
            trackBounds.y(),
            trackBounds.height(),
            getContentHeight(),
            getViewportHeight(),
            visualScrollOffsetY.rounded());
        return new LytRect(trackBounds.x(), thumb.start(), trackBounds.width(), thumb.size());
    }

    private void snapVisualScrollToTarget() {
        visualScrollOffsetY.snapTo(scrollOffsetY);
    }

    private void updateVisualScroll() {
        visualScrollOffsetY.updateTowards(scrollOffsetY, 28f, 0.25f, 0.01f, Math.max(128f, getViewportHeight() * 2f));
    }
}
