package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import lombok.Getter;

@Getter
public class LytList extends LytVBox {

    private final boolean ordered;
    private final int start;

    public LytList(boolean ordered, int start) {
        this.ordered = ordered;
        this.start = start;
        setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
    }

    public int getDepth() {
        int depth = 1;
        for (var node = getParent(); node != null; node = node.getParent()) {
            if (node instanceof LytList) {
                depth++;
            }
        }
        return depth;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Manual layout path — only reached from layoutContentSubtree for Mermaid
        // NodeContent (no Rust pass).  Lay out children (LytListItems) vertically
        // and return accumulated bounds.  Normal document pipeline bypasses this
        // (Rust is the authoritative layout engine).
        int cursorY = y;
        int maxWidth = 0;
        for (LytBlock child : children) {
            var childBounds = child.layout(context, x, cursorY, availableWidth);
            cursorY += childBounds.height();
            maxWidth = Math.max(maxWidth, childBounds.width());
        }
        return new LytRect(x, y, maxWidth, Math.max(0, cursorY - y));
    }

}
