package com.hfstudio.guidenh.guide.document.block.table;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.render.RenderContext;

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
    @Nullable
    private ColorValue cellBackground;

    @Override
    protected boolean allowsContentWidthExpansion() {
        return false;
    }

    @Override
    public void render(RenderContext context) {
        if (cellBackground != null && !bounds.isEmpty()) {
            context.fillRect(
                bounds.x() + 1,
                bounds.y() + 1,
                Math.max(0, bounds.width() - 2),
                Math.max(0, bounds.height() - 2),
                cellBackground);
        }
        LytRect contentBounds = new LytRect(
            bounds.x() + paddingLeft,
            bounds.y() + paddingTop,
            Math.max(0, bounds.width() - paddingLeft - paddingRight),
            Math.max(0, bounds.height() - paddingTop - paddingBottom));
        context.pushLocalScissor(contentBounds);
        try {
            super.render(context);
        } finally {
            context.popScissor();
        }
    }

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

    public void setCellBackground(@Nullable ColorValue background) {
        cellBackground = background;
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
