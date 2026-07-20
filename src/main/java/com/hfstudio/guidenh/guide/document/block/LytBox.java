package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;

import lombok.Setter;

public abstract class LytBox extends LytBlock implements LytBlockContainer {

    protected final List<LytBlock> children = new ArrayList<>();

    @Setter
    protected int paddingLeft;
    @Setter
    protected int paddingTop;
    @Setter
    protected int paddingRight;
    @Setter
    protected int paddingBottom;

    private final BorderRenderer borderRenderer = new BorderRenderer();

    @Nullable
    private SymbolicColor backgroundColor;

    @Override
    public void removeChild(LytNode node) {
        if (node instanceof LytBlock block && block.parent == this) {
            if (isAttached()) LytDocument.notifyDetach(block);
            children.remove(block);
            block.parent = null;
        }
    }

    @Override
    public void append(LytBlock block) {
        if (block.parent != null) {
            block.parent.removeChild(block);
        }
        block.parent = this;
        children.add(block);
        if (isAttached()) LytDocument.notifyAttach(block);
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        if (!(oldChild instanceof LytBlock oldBlock)) return;
        if (!(newChild instanceof LytBlock newBlock)) return;
        int idx = children.indexOf(oldBlock);
        if (idx < 0) return;
        if (isAttached()) LytDocument.notifyDetach(oldBlock);
        oldBlock.parent = null;
        if (newBlock.parent != null) {
            newBlock.parent.removeChild(newBlock);
        }
        newBlock.parent = this;
        children.set(idx, newBlock);
        if (isAttached()) LytDocument.notifyAttach(newBlock);
        LytDocument doc = getDocument();
        if (doc != null) {
            doc.invalidateLayout();
        }
    }

    public void clearContent() {
        for (var child : children) {
            child.parent = null;
        }
        children.clear();
    }

    protected abstract LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth);

    @Override
    protected final LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int borderTop = getBorderTop().width();
        int borderLeft = getBorderLeft().width();
        int borderRight = getBorderRight().width();
        int borderBottom = getBorderBottom().width();

        // Apply padding and border
        var innerLayout = computeBoxLayout(
            context,
            x + paddingLeft + borderLeft,
            y + paddingTop + borderTop,
            availableWidth - paddingLeft - paddingRight - borderLeft - borderRight);

        return innerLayout.expand(
            paddingLeft + borderLeft,
            paddingTop + borderTop,
            paddingRight + borderRight,
            paddingBottom + borderBottom);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var child : children) {
            child.moveLayoutPos(deltaX, deltaY);
        }
    }

    public final void setPadding(int padding) {
        paddingLeft = padding;
        paddingTop = padding;
        paddingRight = padding;
        paddingBottom = padding;
    }

    public @Nullable SymbolicColor getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(@Nullable SymbolicColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return children;
    }

    @Override
    public void render(RenderContext context) {
        if (backgroundColor != null) {
            context.fillRect(bounds, backgroundColor);
        }

        for (var child : children) {
            child.render(context);
        }

        // Only render borders when at least one side has a non-zero width; most boxes have no borders.
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            borderRenderer
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        if (backgroundColor != null) {
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    resolveBackgroundArgb()));
        }
    }

    @Override
    public void emitDecorations(PrimitiveCollector c) {
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            c.emit(
                new GuideRenderPrimitive.DrawBorder(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    getBorderTop().width(),
                    getBorderLeft().width(),
                    getBorderRight().width(),
                    getBorderBottom().width(),
                    resolveBorderArgb()));
        }
    }

    private int resolveBackgroundArgb() {
        if (backgroundColor == null) return 0;
        return backgroundColor.resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current());
    }

    private int resolveBorderArgb() {
        // DrawBorder is single-color; use the first side that declares one
        // (some blocks, e.g. the code toolbar, only set a bottom border).
        BorderStyle[] sides = { getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom() };
        for (BorderStyle side : sides) {
            var color = side.color();
            if (color != null) {
                return color.resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current());
            }
        }
        return 0xFF000000;
    }
}
