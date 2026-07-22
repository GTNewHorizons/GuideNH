package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

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

    /**
     * Bounds used for viewport culling: the union of this block's own bounds
     * and all descendants' bounds. Computed after each external layout pass —
     * floated or otherwise overflowing children must keep their ancestors
     * visible even when the ancestor's own rect leaves the viewport.
     */
    @Nullable
    private LytRect cullBounds;

    /** Culling bounds: subtree union when computed, own bounds otherwise. */
    public LytRect getCullBounds() {
        return cullBounds != null ? cullBounds : bounds;
    }

    public void setCullBounds(@Nullable LytRect cullBounds) {
        this.cullBounds = cullBounds;
    }

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

    /**
     * Flex grow factor for this block inside a row/column flex container
     * (declared by the block itself, e.g. the code toolbar's language label
     * takes the remaining width). Read directly by the layout compiler — no
     * serializer-side special case.
     */
    @Getter
    @Setter
    private float flexGrow;

    /**
     * Override the layout bounds with an externally computed rect (the Rust
     * layout engine). Children receive their own rects from the same pass, so
     * no propagation happens here. Subclasses with position/size-dependent
     * internal state (sample caches, precomputed geometry) should override
     * {@link #onExternalLayoutApplied} to invalidate it.
     */
    public void applyExternalLayout(LytRect rect) {
        LytRect old = bounds;
        bounds = rect;
        onExternalLayoutApplied(old, rect);
    }

    /**
     * The rect this block occupies in the document flow. Unlike
     * {@link #getBounds()} this is never overridden for visual overflow — e.g.
     * {@code LytDocumentFloat} reports a zero-height flow rect while its inner
     * content visually overflows into the following content.
     */
    public LytRect getFlowBounds() {
        return bounds;
    }

    /**
     * Called after {@link #applyExternalLayout} replaced this block's bounds.
     * Default no-op. Subclasses with position/size-dependent internal state
     * (sample caches, precomputed geometry) should override to invalidate it.
     */
    protected void onExternalLayoutApplied(LytRect oldBounds, LytRect newBounds) {}

    /**
     * Called after the <b>entire</b> external-layout writeback pass completed —
     * i.e. when this block's children's bounds are also final. Default no-op.
     * Scroll containers override it to re-apply their scroll offset to the
     * content (the writeback resets content to the unscrolled position).
     */
    protected void afterExternalLayout() {}

    @Override
    public LytRect getBounds() {
        return bounds;
    }

    public boolean isCulled(LytRect viewport) {
        return !viewport.intersects(getCullBounds());
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
            // The cull bounds (subtree union) move rigidly with the block —
            // scroll replay/smooth scrolling move the whole subtree by the same
            // delta, so a fresh union is unnecessary; without this, content
            // scrolled into view gets culled by its stale rect (B-1).
            if (cullBounds != null) {
                cullBounds = cullBounds.move(deltaX, deltaY);
            }
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
     * Whether this block renders through {@link #computePrimitives}. When
     * {@code false}, {@link PrimitiveCollector} emits a legacy-render fallback
     * ({@link GuideRenderPrimitive.HostDraw}) that invokes {@link #render} for
     * the whole subtree and does not recurse into children.
     * <p>
     * Defaults to {@code false} so unmigrated blocks keep rendering via the
     * legacy path; subclasses that implement {@code computePrimitives} must
     * override this to return {@code true}. The decision may be dynamic
     * (e.g. LytParagraph returns true only when a Rust-shaped glyph run is
     * available).
     */
    public boolean usePrimitives() {
        return false;
    }

    /**
     * Emit this block's own draw primitives. <b>Do not</b> iterate
     * {@link #getChildren()} — the {@link PrimitiveCollector} handles
     * tree traversal. Nodes with private rendering data (e.g. MermaidCanvas)
     * may call {@link PrimitiveCollector#collectFrom} on their internal
     * subtrees here.
     */
    public void computePrimitives(PrimitiveCollector c) {}

    /**
     * Override to declare a clip rectangle for this block's children.
     * The PrimitiveCollector will automatically emit PushScissor before
     * recursing into children and PopScissor after, using the returned
     * rectangle in document coordinates.
     *
     * @return a clip rect in document coordinates, or null for no clipping
     */
    public @org.jetbrains.annotations.Nullable LytRect getChildrenClipRect() {
        return null;
    }

    /**
     * Emit decoration primitives (borders, outlines) that must paint
     * <em>after</em> children. Called by {@link PrimitiveCollector#collectFrom}
     * after recursing into {@link #getChildren()}.
     */
    public void emitDecorations(PrimitiveCollector c) {}
}
