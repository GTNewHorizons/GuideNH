package com.hfstudio.guidenh.guide.document.block.table;

import java.util.Locale;

import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytVBox;

/**
 * A cell in a {@link LytTable}s {@link LytTableRow}.
 */
public class LytTableCell extends LytVBox {

    final LytTable table;
    final LytTableRow row;
    final int rowIndex;
    final int columnIndex;
    final int columnSpan;
    final int rowSpan;
    private AlignItems verticalAlignment = AlignItems.START;

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setVerticalAlignment(String value) {
        verticalAlignment = switch (value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT)) {
            case "middle", "center" -> AlignItems.CENTER;
            case "bottom", "end" -> AlignItems.END;
            default -> AlignItems.START;
        };
    }

    public void alignContentVertically(int allocatedHeight) {
        int targetHeight = Math.max(bounds.height(), allocatedHeight);
        int contentTop = bounds.y() + paddingTop;
        int contentBottom = bounds.y() + targetHeight - paddingBottom;
        int contentHeight = Math.max(0, contentBottom - contentTop);
        int firstTop = Integer.MAX_VALUE;
        int lastBottom = Integer.MIN_VALUE;
        for (var child : children) {
            var childBounds = child.getBounds();
            if (childBounds == null) {
                continue;
            }
            firstTop = Math.min(firstTop, childBounds.y());
            lastBottom = Math.max(lastBottom, childBounds.bottom());
        }
        if (firstTop == Integer.MAX_VALUE) {
            bounds = bounds.withHeight(targetHeight);
            return;
        }
        int contentUsed = Math.max(0, lastBottom - firstTop);
        int freeSpace = Math.max(0, contentHeight - contentUsed);
        int offset = switch (verticalAlignment) {
            case CENTER -> freeSpace / 2;
            case END -> freeSpace;
            case START -> 0;
        };
        if (offset != 0) {
            for (var child : children) {
                child.moveLayoutPos(0, offset);
            }
        }
        bounds = bounds.withHeight(targetHeight);
    }

    public LytTableCell(LytTable table, LytTableRow row, LytTableColumn column) {
        this(table, row, row.rowIndex, table.indexOfColumn(column), 1, 1);
    }

    public LytTableCell(LytTable table, LytTableRow row, int rowIndex, int columnIndex, int columnSpan, int rowSpan) {
        this.table = table;
        this.row = row;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.columnSpan = columnSpan;
        this.rowSpan = rowSpan;
        this.parent = row;

        paddingLeft = 1;
        paddingTop = 1;
        paddingRight = 1;
        paddingBottom = 1;
    }
}
