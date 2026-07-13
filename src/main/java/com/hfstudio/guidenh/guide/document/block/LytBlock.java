package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytPoint;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;

import lombok.Getter;
import lombok.Setter;

public abstract class LytBlock extends LytNode {

    /**
     * Content rectangle.
     */
    protected LytRect bounds = LytRect.empty();

    @Getter
    @Setter
    private int marginTop;
    @Getter
    @Setter
    private int marginLeft;
    @Getter
    @Setter
    private int marginRight;
    @Getter
    @Setter
    private int marginBottom;

    @Getter
    @Setter
    private BorderStyle borderTop = BorderStyle.NONE;
    @Getter
    @Setter
    private BorderStyle borderLeft = BorderStyle.NONE;
    @Getter
    @Setter
    private BorderStyle borderRight = BorderStyle.NONE;
    @Getter
    @Setter
    private BorderStyle borderBottom = BorderStyle.NONE;

    /**
     * Always expand this block to the full available width.
     */
    @Getter
    @Setter
    private boolean fullWidth;

    @Override
    public LytRect getBounds() {
        return bounds;
    }

    public boolean isCulled(LytRect viewport) {
        return !viewport.intersects(bounds);
    }

    public final void setLayoutPos(LytPoint point) {
        int newX = (int) point.x();
        int newY = (int) point.y();
        int deltaX = newX - bounds.x();
        int deltaY = newY - bounds.y();
        if (deltaX != 0 || deltaY != 0) {
            bounds = bounds.move(deltaX, deltaY);
            onLayoutMoved(deltaX, deltaY);
        }
    }

    /**
     * Shifts this block's layout position by the given delta without requiring a {@link LytPoint} allocation.
     * Prefer this over {@link #setLayoutPos} when the caller already has the delta (e.g. inside
     * {@link #onLayoutMoved} implementations propagating a parent's move to children).
     */
    public final void moveLayoutPos(int deltaX, int deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            bounds = bounds.move(deltaX, deltaY);
            onLayoutMoved(deltaX, deltaY);
        }
    }

    public final LytRect layout(LayoutContext context, int x, int y, int availableWidth) {
        bounds = computeLayout(context, x, y, availableWidth);
        if (fullWidth && bounds.width() < availableWidth) {
            bounds = bounds.withWidth(availableWidth);
        }
        return bounds;
    }

    public int getMarginStart(LytAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> getMarginLeft();
            case VERTICAL -> getMarginTop();
        };
    }

    public int getMarginEnd(LytAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> getMarginRight();
            case VERTICAL -> getMarginBottom();
        };
    }

    public void setBorder(BorderStyle style) {
        setBorderTop(style);
        setBorderLeft(style);
        setBorderRight(style);
        setBorderBottom(style);
    }

    protected abstract LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth);

    /**
     * Implement to react to layout previously computed by {@link #computeLayout} being moved.
     */
    protected abstract void onLayoutMoved(int deltaX, int deltaY);

    public abstract void render(RenderContext context);

    // ---- explicit size ---------------------------------------------------

    /**
     * Override to declare a preferred width in pixels.
     * Returns -1 when no explicit width is set.
     */
    public int getExplicitWidth() {
        return -1;
    }

    /**
     * Override to declare a preferred height in pixels.
     * Returns -1 when no explicit height is set.
     */
    public int getExplicitHeight() {
        return -1;
    }

    // ---- primitive collection --------------------------------------------

    /**
     * Emit this block's own draw primitives. <b>Do not</b> iterate
     * {@link #getChildren()} — the {@link PrimitiveCollector} handles
     * tree traversal. Nodes with private rendering data (e.g. MermaidCanvas)
     * may call {@link PrimitiveCollector#collectFrom} on their internal
     * subtrees here.
     */
    public void computePrimitives(PrimitiveCollector c) {}

    /**
     * Emit decoration primitives (borders, outlines) that must paint
     * <em>after</em> children. Called by {@link PrimitiveCollector#collectFrom}
     * after recursing into {@link #getChildren()}.
     */
    public void emitDecorations(PrimitiveCollector c) {}
}
