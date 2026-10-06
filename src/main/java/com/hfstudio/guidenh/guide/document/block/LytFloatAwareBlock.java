package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * Wraps a block and constrains it to the currently available horizontal lane after document
 * floats have claimed space on the left or right side.
 *
 * <p>
 * This is intended for block-level content such as tables or callout boxes whose own layout does
 * not participate in inline flow layout. Paragraphs already consult {@link LayoutContext}
 * directly, so they do not need this wrapper.
 */
@Getter
public class LytFloatAwareBlock extends LytBlock {

    private final LytBlock inner;

    public LytFloatAwareBlock(LytBlock inner) {
        this.inner = inner;
        inner.parent = this;
        setFullWidth(inner.isFullWidth());
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of(inner);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int laneY = y;
        while (true) {
            int laneLeft = context.getLeftFloatRightEdgeOr(x);
            int laneRight = context.getRightFloatLeftEdgeOr(x + availableWidth);
            int laneWidth = Math.max(0, laneRight - laneLeft);
            if (laneWidth > 0) {
                return inner.layout(context, laneLeft, laneY, laneWidth);
            }

            var nextFloatBottom = context.getNextFloatBottomEdge(laneY);
            if (nextFloatBottom.isEmpty()) {
                return inner.layout(context, x, laneY, availableWidth);
            }

            laneY = nextFloatBottom.getAsInt();
            context.clearFloatsAbove(laneY);
        }
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        inner.moveLayoutPos(deltaX, deltaY);
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        return inner.pickNode(x, y);
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        // No-op: inner child is picked up by PrimitiveCollector.collectFrom traversal.
    }

    @Override
    public void render(RenderContext context) {
        inner.render(context);
    }
}
