package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LytListItem extends LytVBox {

    public static final int LEVEL_MARGIN = 10;

    /**
     * Shared gutter marker tint: muted gray so markers recede from body text.
     * Declared into the layout input ({@code ListMarkerData.style.color}); the
     * Rust pipeline shapes and positions the marker glyphs and renders them
     * through the shared glyph-quad stream. (The legacy bullet and
     * ordered-number draw paths in this class are dead.)
     */
    public static final ColorValue COL_MARKER = ColorUtils.MC_GRAY;

    private final ResolvedTextStyle style = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);

    public LytListItem() {
        // paddingLeft is read by the Rust layout engine and creates the content
        // indentation (replaces the legacy computeBoxLayout's x+margin pass).
        // Markers are Rust-shaped and right-aligned to the item's own left edge
        // (the per-level document text line) in the layout pipeline.
        setPaddingLeft(LEVEL_MARGIN);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Manual layout path: only reached from layoutContentSubtree for Mermaid
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

    /**
     * The shared gutter marker text declared into the layout input
     * ({@code ListMarkerData}) for the Rust pipeline to shape and draw: ordered
     * {@code "N."} or unordered {@code "•"}. Returns {@code null} for items
     * that draw their own gutter marker ({@link #hasOwnMarker()}, e.g. the task
     * checkbox), so the shared slot is never double-declared. Marker glyphs are
     * emitted by Rust as ordinary glyph quads (no Java draw path, zero
     * special-casing).
     *
     * <p>
     * Ordered numbers are computed on demand at call time
     * ({@link #computeOrderedNumber()}, an O(N) sibling scan that honours the
     * list's start value). Serialization runs before
     * {@code afterExternalLayout()}, so a number cached during layout is still
     * unavailable on the first serialization pass.
     */
    public String getSharedMarkerText() {
        if (hasOwnMarker()) {
            return null;
        }
        int number = computeOrderedNumber();
        return number >= 0 ? number + "." : "•";
    }

    /**
     * Computes the ordered number on demand: when the parent is a
     * {@link LytList} with {@code isOrdered()}, the number is derived by
     * counting the parent list's sibling {@link LytListItem} entries from
     * {@link LytList#getStart()} up to this item; otherwise (unordered list, or
     * no list parent) it returns -1. O(N) in the item count, which is
     * acceptable for one pass per layout serialization.
     */
    private int computeOrderedNumber() {
        if (parent instanceof LytList list && list.isOrdered()) {
            int number = list.getStart();
            for (var child : list.getChildren()) {
                if (child == this) break;
                if (child instanceof LytListItem) number++;
            }
            return number;
        }
        return -1;
    }

    /**
     * Whether this list item draws its own gutter marker (e.g. the task
     * checkbox) instead of the shared bullet / ordered number. Subclasses with
     * a custom marker must override to return {@code true} so the Rust
     * serializer skips the shared marker slot ({@link #getSharedMarkerText()}
     * returns null, otherwise both would double-draw).
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

    /** Context-free overload for use in primitive collection. */
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
