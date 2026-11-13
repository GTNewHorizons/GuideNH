package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * Aligns one block within the available width. LEFT delegates directly; CENTER and RIGHT measure natural
 * width before a second layout pass applies the horizontal offset.
 */
@Getter
public class LytAlignedBlock extends LytBlock {

    private LytBlock inner;
    private final ContentAlign align;

    /**
     * @param inner the block to align
     * @param align the desired horizontal alignment
     */
    public LytAlignedBlock(LytBlock inner, ContentAlign align) {
        this.inner = inner;
        inner.parent = this;
        this.align = align;
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
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (align == ContentAlign.LEFT) {
            return inner.layout(context, x, y, availableWidth);
        }

        var naturalBounds = inner.layout(context, x, y, availableWidth);
        int innerWidth = naturalBounds.width();

        int newX;
        if (align == ContentAlign.CENTER) {
            newX = x + (availableWidth - innerWidth) / 2;
        } else {
            newX = x + availableWidth - innerWidth;
        }

        if (newX == x) {
            return naturalBounds;
        }

        inner.layout(context, newX, y, innerWidth);
        return inner.getBounds();
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
