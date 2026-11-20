package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * Registers left or right float bounds in the shared LayoutContext so subsequent text wraps around its
 * content. Reports zero height to let the following block start beside the float.
 */
@Getter
public class LytDocumentFloat extends LytBlock {

    private static final int FLOAT_GAP = 5;

    private LytBlock inner;
    private final boolean floatRight;

    /**
     * @param inner      the block to float
     * @param floatRight {@code true} to float to the right, {@code false} to float to the left
     */
    public LytDocumentFloat(LytBlock inner, boolean floatRight) {
        this.inner = inner;
        inner.parent = this;
        this.floatRight = floatRight;
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        if (oldChild != inner || !(newChild instanceof LytBlock)) return;
        inner.parent = null;
        inner = (LytBlock) newChild;
        inner.parent = this;
        invalidateLayout();
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of(inner);
    }

    @Override
    public boolean isCulled(LytRect viewport) {
        return inner.isCulled(viewport);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (floatRight) {
            var naturalBounds = inner.layout(context, x, y, availableWidth);
            int innerWidth = naturalBounds.width();
            int rx = x + availableWidth - innerWidth;
            inner.layout(context, rx, y, innerWidth);
            context.addRightFloat(
                new LytRect(rx - FLOAT_GAP, y, innerWidth + FLOAT_GAP, naturalBounds.height() + FLOAT_GAP));
        } else {
            var innerBounds = inner.layout(context, x, y, availableWidth);
            context.addLeftFloat(new LytRect(x, y, innerBounds.width() + FLOAT_GAP, innerBounds.height() + FLOAT_GAP));
        }
        return new LytRect(x, y, 0, 0);
    }

    @Override
    public LytRect getBounds() {
        return inner != null ? inner.getBounds() : super.getBounds();
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        inner.moveLayoutPos(deltaX, deltaY);
    }

    @Override
    public LytNode pickNode(int x, int y) {
        return inner.pickNode(x, y);
    }

    @Override
    public void render(RenderContext context) {
        inner.render(context);
    }
}
