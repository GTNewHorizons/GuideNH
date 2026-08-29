package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LytListItem extends LytVBox {

    public static final int LEVEL_MARGIN = 10;
    private static final int BULLET_SIZE = 3;
    /**
     * Shared marker gutter: both the unordered bullet and the ordered number
     * right-align to this line (bounds.x() + LEVEL_MARGIN - MARKER_GUTTER_OFFSET),
     * so every marker hangs from one vertical line and the text starts uniformly
     * at bounds.x() + LEVEL_MARGIN (the item's content box via paddingLeft).
     */
    private static final int MARKER_GUTTER_OFFSET = 5;

    private final ResolvedTextStyle style = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);

    /**
     * Cached ordered item number from the last layout pass.
     * -1 means the item is in an unordered list or has no list parent.
     * Updated in {@link #computeBoxLayout} so that {@link #render} avoids an O(N) sibling scan every frame.
     */
    private int cachedOrderedNumber = -1;

    public LytListItem() {
        // paddingLeft is read by the Rust layout engine and creates the content
        // indentation (replaces the legacy computeBoxLayout's x+margin pass).
        // Markers are drawn relative to the border box (getBounds()) and land in
        // the padding slot left of the content — matching the old render() semantics.
        setPaddingLeft(LEVEL_MARGIN);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Manual layout path — only reached from layoutContentSubtree for Mermaid
        // NodeContent (no Rust pass). paddingLeft (LEVEL_MARGIN) is already
        // applied by LytBox.computeLayout before this method; the extra margin
        // below creates content indentation leaving the bullet/number zone
        // visible. Normal document pipeline bypasses this (Rust is authoritative).
        var margin = LEVEL_MARGIN;
        int cursorY = y;
        int contentAvailWidth = Math.max(1, availableWidth - margin);
        int maxContentWidth = 0;
        for (LytBlock child : children) {
            var childBounds = child.layout(context, x + margin, cursorY, contentAvailWidth);
            cursorY += childBounds.height();
            maxContentWidth = Math.max(maxContentWidth, childBounds.width());
        }
        int contentHeight = Math.max(0, cursorY - y);
        return new LytRect(x, y, maxContentWidth + margin, contentHeight);
    }

    @Override
    protected void afterExternalLayout() {
        super.afterExternalLayout();
        // Compute ordered item number from parent list (rendering reads
        // cachedOrderedNumber; avoid sibling scan every frame).
        if (parent instanceof LytList list && list.isOrdered()) {
            int number = list.getStart();
            for (var child : list.getChildren()) {
                if (child == this) break;
                if (child instanceof LytListItem) number++;
            }
            cachedOrderedNumber = number;
        } else {
            cachedOrderedNumber = -1;
        }
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);
        if (hasOwnMarker()) {
            // Subclasses with a custom gutter marker (e.g. the task checkbox)
            // draw it themselves; skip the shared bullet/number so both never
            // paint the same slot.
            return;
        }
        if (cachedOrderedNumber >= 0) {
            String label = cachedOrderedNumber + ".";
            int width = GuideText.measureWidth(label, style);
            var bounds = getBounds();
            var markerLine = getMarkerLineBounds();
            // Right-aligned to the shared marker gutter: the number's right
            // edge lands on bounds.x() + LEVEL_MARGIN - MARKER_GUTTER_OFFSET,
            // the same hanging line the unordered bullet right-aligns to.
            int x = bounds.x() + LEVEL_MARGIN - width - MARKER_GUTTER_OFFSET;
            c.emit(new GuideRenderPrimitive.DrawText(label, x, markerLine.y(), style));
        } else {
            var bounds = getBounds();
            var markerLine = getMarkerLineBounds();
            int bulletY = markerLine.y() + (markerLine.height() - BULLET_SIZE) / 2;
            int argb = SymbolicColor.BODY_TEXT.resolve(LightDarkMode.current());
            // Right-align the bullet to the same hanging line as ordered
            // numbers (LEVEL_MARGIN - MARKER_GUTTER_OFFSET), so both marker
            // types share one gutter and text starts uniformly at LEVEL_MARGIN.
            int bulletX = bounds.x() + LEVEL_MARGIN - MARKER_GUTTER_OFFSET - BULLET_SIZE;
            c.emit(new GuideRenderPrimitive.FillRect(bulletX, bulletY, BULLET_SIZE, BULLET_SIZE, argb));
        }
    }

    @Override
    public void render(RenderContext context) {
        if (!hasOwnMarker()) {
            if (cachedOrderedNumber >= 0) {
                String label = cachedOrderedNumber + ".";
                var width = context.getWidth(label, style);
                var bounds = getBounds();
                var markerLine = getMarkerLineBounds(context);
                // Same shared-gutter anchor as computePrimitives: right edge at
                // bounds.x() + LEVEL_MARGIN - MARKER_GUTTER_OFFSET.
                var x = bounds.x() + LEVEL_MARGIN - width - MARKER_GUTTER_OFFSET;
                context.drawText(label, x, markerLine.y(), style);
            } else {
                var bounds = getBounds();
                var markerLine = getMarkerLineBounds(context);
                int bulletY = markerLine.y() + (markerLine.height() - BULLET_SIZE) / 2;
                int bulletX = bounds.x() + LEVEL_MARGIN - MARKER_GUTTER_OFFSET - BULLET_SIZE;
                context.fillRect(bulletX, bulletY, BULLET_SIZE, BULLET_SIZE, SymbolicColor.BODY_TEXT);
            }
        }
        super.render(context);
    }

    /**
     * Whether this list item draws its own gutter marker (e.g. the task
     * checkbox) instead of the shared bullet / ordered number. Subclasses with
     * a custom marker must override to return {@code true} so {@link
     * #computePrimitives(PrimitiveCollector)} and {@link #render(RenderContext)}
     * skip the shared marker slot (both would otherwise double-draw).
     */
    protected boolean hasOwnMarker() {
        return false;
    }

    protected LytRect getMarkerLineBounds(RenderContext context) {
        if (!children.isEmpty()) {
            LytBlock firstChild = children.getFirst();
            if (firstChild instanceof LytParagraph paragraph) {
                LytRect firstTextRun = paragraph.getFirstTextRunBounds();
                if (firstTextRun != null) {
                    return firstTextRun;
                }
                LytRect firstLine = paragraph.getFirstTextRunBounds();
                if (firstLine != null) {
                    return new LytRect(firstLine.x(), firstLine.y(), firstLine.width(), context.getLineHeight(style));
                }
            }
            return firstChild.getBounds();
        }
        LytRect bounds = getBounds();
        return new LytRect(bounds.x(), bounds.y(), bounds.width(), context.getLineHeight(style));
    }

    /** Context-free overload for use in {@link #computePrimitives}. */
    protected LytRect getMarkerLineBounds() {
        if (!children.isEmpty()) {
            LytBlock firstChild = children.getFirst();
            if (firstChild instanceof LytParagraph paragraph) {
                LytRect firstTextRun = paragraph.getFirstTextRunBounds();
                if (firstTextRun != null) {
                    return firstTextRun;
                }
                LytRect firstLine = paragraph.getFirstTextRunBounds();
                if (firstLine != null) {
                    return new LytRect(firstLine.x(), firstLine.y(), firstLine.width(), GuideText.lineHeight(style));
                }
            }
            return firstChild.getBounds();
        }
        LytRect bounds = getBounds();
        return new LytRect(bounds.x(), bounds.y(), bounds.width(), GuideText.lineHeight(style));
    }
}
