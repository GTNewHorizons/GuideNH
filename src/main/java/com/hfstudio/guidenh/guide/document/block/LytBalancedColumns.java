package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;

import lombok.Getter;
import lombok.Setter;

/**
 * Places children into the shortest available column while preserving each child's measured width.
 *
 * <p>
 * The highest column count whose measured column widths fit the available width is selected, up to
 * {@link #getMaxColumns()}. If even two columns cannot fit, children are laid out as a vertical stack.
 */
@Getter
@Setter
public class LytBalancedColumns extends LytBox {

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

        int[] itemWidths = new int[count];
        int[] itemHeights = new int[count];
        for (int i = 0; i < count; i++) {
            LytBlock child = children.get(i);
            LytRect measured = child.layout(context, x, y, availableWidth);
            itemWidths[i] = Math.max(1, measured.width() + child.getMarginLeft() + child.getMarginRight());
            itemHeights[i] = Math.max(1, measured.height() + child.getMarginTop() + child.getMarginBottom());
        }

        int columns = Math.min(count, maxColumns);
        while (columns > 1) {
            int[] assignments = new int[count];
            int[] columnWidths = new int[columns];
            int[] columnBottoms = new int[columns];
            for (int i = 0; i < count; i++) {
                int column = shortestColumn(columnBottoms);
                assignments[i] = column;
                columnWidths[column] = Math.max(columnWidths[column], itemWidths[i]);
                columnBottoms[column] += itemHeights[i] + gap;
            }
            int requiredWidth = Math.max(0, columns - 1) * gap;
            for (int width : columnWidths) {
                requiredWidth += width;
            }
            if (requiredWidth <= availableWidth) {
                return layoutColumns(context, x, y, availableWidth, assignments, columnWidths, columns);
            }
            columns--;
        }
        return verticalStack(context, x, y, availableWidth);
    }

    private LytRect layoutColumns(LayoutContext context, int x, int y, int availableWidth, int[] assignments,
        int[] columnWidths, int columns) {
        int[] columnBottoms = new int[columns];
        LytBlock[] previousBlocks = new LytBlock[columns];
        int contentWidth = 0;
        int contentHeight = 0;
        int[] columnX = new int[columns];
        for (int i = 1; i < columns; i++) {
            columnX[i] = columnX[i - 1] + columnWidths[i - 1] + gap;
        }
        for (int i = 0; i < children.size(); i++) {
            LytBlock child = children.get(i);
            int column = assignments[i];
            int blockWidth = Math.max(1, columnWidths[column] - child.getMarginLeft() - child.getMarginRight());
            int childX = x + columnX[column] + child.getMarginLeft();
            int childY = Layouts
                .offsetIntoContentArea(LytAxis.VERTICAL, y + columnBottoms[column], previousBlocks[column], child);
            LytRect childBounds = child.layout(context, childX, childY, blockWidth);
            columnBottoms[column] = childBounds.bottom() - y + child.getMarginBottom() + gap;
            previousBlocks[column] = child;
            contentWidth = Math.max(contentWidth, childBounds.right() - x);
            contentHeight = Math.max(contentHeight, childBounds.bottom() - y);
        }
        return new LytRect(x, y, Math.min(availableWidth, contentWidth), contentHeight);
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
