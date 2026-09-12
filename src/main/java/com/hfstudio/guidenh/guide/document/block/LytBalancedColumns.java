package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;

import lombok.Getter;
import lombok.Setter;

/**
 * Places children into columns as wide as the widest of them, filling the shortest column first.
 *
 * <p>
 * Column count is based on the widest child and limited by {@link #getMaxColumns()}.
 * Children wider than a column are laid out at their own width, falling back to a vertical stack.
 */
@Getter
@Setter
public class LytBalancedColumns extends LytBox {

    /** The most columns a page will show, so a row of small children does not become a wall of them. */
    public static final int DEFAULT_MAX_COLUMNS = 4;

    private int gap;

    private int maxColumns = DEFAULT_MAX_COLUMNS;

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int count = children.size();
        if (count == 0) {
            return new LytRect(x, y, 0, 0);
        }
        if (count == 1 || maxColumns <= 1) {
            return verticalStack(context, x, y, availableWidth);
        }

        int widest = widestChildWidth(context, x, y, availableWidth);
        int columns = columnCountFor(availableWidth, widest);
        if (columns <= 1) {
            return verticalStack(context, x, y, availableWidth);
        }

        int columnWidth = widest;
        int[] columnBottoms = new int[columns];
        LytBlock[] previousBlocks = new LytBlock[columns];
        int contentWidth = 0;
        int contentHeight = 0;

        for (LytBlock child : children) {
            int blockWidth = Math.max(1, columnWidth - child.getMarginLeft() - child.getMarginRight());
            int columnIndex = shortestColumn(columnBottoms);

            int columnX = x + columnIndex * (columnWidth + gap) + child.getMarginLeft();
            int columnY = Layouts.offsetIntoContentArea(
                LytAxis.VERTICAL,
                y + columnBottoms[columnIndex],
                previousBlocks[columnIndex],
                child);
            LytRect childBounds = child.layout(context, columnX, columnY, blockWidth);

            columnBottoms[columnIndex] = childBounds.bottom() - y + child.getMarginBottom() + gap;
            previousBlocks[columnIndex] = child;
            contentWidth = Math.max(contentWidth, childBounds.right() - x);
            contentHeight = Math.max(contentHeight, childBounds.bottom() - y);
        }

        return new LytRect(x, y, contentWidth, contentHeight);
    }

    private int widestChildWidth(LayoutContext context, int x, int y, int availableWidth) {
        int widest = 0;
        for (LytBlock child : children) {
            LytRect bounds = child.layout(context, x, y, availableWidth);
            widest = Math.max(widest, bounds.width() + child.getMarginLeft() + child.getMarginRight());
        }
        return widest;
    }

    private int columnCountFor(int availableWidth, int widest) {
        int limit = Math.min(children.size(), maxColumns);
        if (widest <= 0) {
            return Math.max(1, limit);
        }
        // Columns are as wide as the widest child, so n of them need n widths and n-1 gaps.
        int byWidth = (availableWidth + gap) / (widest + gap);
        return Math.max(1, Math.min(limit, byWidth));
    }

    private static int shortestColumn(int[] columnBottoms) {
        int best = 0;
        for (int i = 1; i < columnBottoms.length; i++) {
            if (columnBottoms[i] < columnBottoms[best]) {
                best = i;
            }
        }
        return best;
    }

    private LytRect verticalStack(LayoutContext context, int x, int y, int availableWidth) {
        return Layouts.verticalLayout(context, children, x, y, availableWidth, 0, 0, 0, 0, gap, AlignItems.START);
    }
}
