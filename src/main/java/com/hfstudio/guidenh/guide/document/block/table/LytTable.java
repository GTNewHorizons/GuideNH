package com.hfstudio.guidenh.guide.document.block.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytTable extends LytBlock {

    /**
     * Width of border around cells.
     */
    public static final int CELL_BORDER = 1;
    private final List<LytTableRow> rows = new ArrayList<>();
    private int preferredWidth;
    private int preferredWidthPercent;
    private int borderWidth = CELL_BORDER;
    private int cellSpacing;
    private boolean outerBorder;
    private String horizontalAlignment = "left";

    @Getter
    private final List<LytTableColumn> columns = new ArrayList<>();

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (columns.isEmpty()) {
            return LytRect.empty();
        }

        int tableWidth = preferredWidth > 0 ? Math.min(preferredWidth, availableWidth)
            : preferredWidthPercent > 0 ? Math.max(1, availableWidth * preferredWidthPercent / 100) : availableWidth;
        int alignedX = switch (horizontalAlignment) {
            case "center" -> x + Math.max(0, availableWidth - tableWidth) / 2;
            case "right" -> x + Math.max(0, availableWidth - tableWidth);
            default -> x;
        };
        layoutColumns(alignedX, tableWidth);

        int[] rowHeights = new int[rows.size()];
        for (var row : rows) {
            for (var cell : row.getChildren()) {
                int cellWidth = getSpanWidth(cell.columnIndex, cell.columnSpan);
                LytRect measured = cell.layout(context, columns.get(cell.columnIndex).x, 0, cellWidth);
                if (cell.rowSpan == 1) {
                    rowHeights[cell.rowIndex] = Math.max(rowHeights[cell.rowIndex], measured.height());
                }
            }
        }
        for (var row : rows) {
            for (var cell : row.getChildren()) {
                if (cell.rowSpan <= 1) {
                    continue;
                }
                int rowEnd = Math.min(rows.size(), cell.rowIndex + cell.rowSpan);
                int availableHeight = 0;
                for (int rowIndex = cell.rowIndex; rowIndex < rowEnd; rowIndex++) {
                    availableHeight += rowHeights[rowIndex];
                    if (rowIndex > cell.rowIndex) {
                        availableHeight += getRowGap();
                    }
                }
                int requiredHeight = cell.getBounds()
                    .height();
                if (requiredHeight > availableHeight && rowEnd > cell.rowIndex) {
                    rowHeights[rowEnd - 1] += requiredHeight - availableHeight;
                }
            }
        }

        int currentY = y + getOuterInset();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = rows.get(rowIndex);
            int rowTop = currentY;
            for (var cell : row.getChildren()) {
                int cellWidth = getSpanWidth(cell.columnIndex, cell.columnSpan);
                cell.layout(context, columns.get(cell.columnIndex).x, rowTop, cellWidth);
                cell.alignContentVertically(getSpanHeight(cell.rowIndex, cell.rowSpan, rowHeights));
            }
            row.bounds = new LytRect(alignedX, rowTop, tableWidth, rowHeights[rowIndex]);
            currentY = row.bounds.bottom() + getRowGap();
        }

        int tableBottom = rows.isEmpty() ? y + getOuterInset() : rows.getLast().bounds.bottom() + getOuterInset();
        return new LytRect(alignedX, y, tableWidth, Math.max(0, tableBottom - y));
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var col : columns) {
            col.x += deltaX;
        }
        for (var row : rows) {
            row.bounds = row.bounds.move(deltaX, deltaY);
            for (var cell : row.getChildren()) {
                cell.moveLayoutPos(deltaX, deltaY);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (outerBorder && borderWidth > 0 && !bounds.isEmpty()) {
            context.fillRect(bounds.x(), bounds.y(), bounds.width(), borderWidth, ColorUtils.TABLE_BORDER);
            context.fillRect(
                bounds.x(),
                bounds.bottom() - borderWidth,
                bounds.width(),
                borderWidth,
                ColorUtils.TABLE_BORDER);
            context.fillRect(bounds.x(), bounds.y(), borderWidth, bounds.height(), ColorUtils.TABLE_BORDER);
            context.fillRect(
                bounds.right() - borderWidth,
                bounds.y(),
                borderWidth,
                bounds.height(),
                ColorUtils.TABLE_BORDER);
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = rows.get(rowIndex);
            for (int columnBoundary = 0; columnBoundary < columns.size() - 1; columnBoundary++) {
                if (hasCellCrossingColumnBoundary(rowIndex, columnBoundary)) {
                    continue;
                }
                int borderX = getColumnBoundary(columns.get(columnBoundary));
                int segmentHeight = row.bounds.height();
                if (rowIndex + 1 < rows.size() && !hasCellCrossingColumnBoundary(rowIndex + 1, columnBoundary)) {
                    segmentHeight += cellSpacing;
                }
                context.fillRect(borderX, row.bounds.y(), borderWidth, segmentHeight, ColorUtils.TABLE_BORDER);
            }
            if (rowIndex + 1 < rows.size()) {
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    if (hasCellCrossingRowBoundary(rowIndex, columnIndex)) {
                        continue;
                    }
                    var column = columns.get(columnIndex);
                    context.fillRect(
                        column.x,
                        getRowBoundary(row),
                        column.width + getCellSpacingOverlap(columnIndex),
                        borderWidth,
                        ColorUtils.TABLE_BORDER);
                }
            }
        }

        for (var row : rows) {
            for (var cell : row.getChildren()) {
                cell.render(context);
            }
        }
    }

    public LytTableRow appendRow() {
        var row = new LytTableRow(this, rows.size());
        rows.add(row);
        return row;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
        this.preferredWidthPercent = 0;
        invalidateLayout();
    }

    public void setPreferredWidthPercent(int preferredWidthPercent) {
        this.preferredWidthPercent = Math.max(0, Math.min(100, preferredWidthPercent));
        this.preferredWidth = 0;
        invalidateLayout();
    }

    public void setHorizontalAlignment(String horizontalAlignment) {
        String normalized = horizontalAlignment == null ? "left"
            : horizontalAlignment.trim()
                .toLowerCase(Locale.ROOT);
        this.horizontalAlignment = switch (normalized) {
            case "center", "right" -> normalized;
            default -> "left";
        };
        invalidateLayout();
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(0, Math.min(16, borderWidth));
        invalidateLayout();
    }

    public void setCellSpacing(int cellSpacing) {
        this.cellSpacing = Math.max(0, Math.min(64, cellSpacing));
        invalidateLayout();
    }

    public void setOuterBorder(boolean outerBorder) {
        this.outerBorder = outerBorder;
        invalidateLayout();
    }

    public boolean isOccupied(int rowIndex, int columnIndex, int columnSpan) {
        for (var row : rows) {
            for (var cell : row.getChildren()) {
                if (rowIndex >= cell.rowIndex && rowIndex < cell.rowIndex + cell.rowSpan
                    && columnIndex < cell.columnIndex + cell.columnSpan
                    && columnIndex + columnSpan > cell.columnIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getSpanWidth(int columnIndex, int columnSpan) {
        int width = Math.max(0, columnSpan - 1) * getColumnGap();
        for (int index = columnIndex; index < columnIndex + columnSpan; index++) {
            width += columns.get(index).width;
        }
        return width;
    }

    private int getSpanHeight(int rowIndex, int rowSpan, int[] rowHeights) {
        int end = Math.min(rowHeights.length, rowIndex + rowSpan);
        int height = 0;
        for (int index = rowIndex; index < end; index++) {
            height += rowHeights[index];
            if (index > rowIndex) {
                height += getRowGap();
            }
        }
        return height;
    }

    private int getColumnGap() {
        return cellSpacing;
    }

    private int getRowGap() {
        return cellSpacing;
    }

    private int getOuterInset() {
        return outerBorder ? borderWidth : 0;
    }

    private int getColumnBoundary(LytTableColumn column) {
        return column.x + column.width + cellSpacing / 2;
    }

    private int getRowBoundary(LytTableRow row) {
        return row.bounds.bottom() + cellSpacing / 2;
    }

    private int getCellSpacingOverlap(int columnIndex) {
        return columnIndex + 1 < columns.size() ? cellSpacing : 0;
    }

    private boolean hasCellCrossingColumnBoundary(int rowIndex, int boundary) {
        for (var row : rows) {
            for (var cell : row.getChildren()) {
                if (rowIndex >= cell.rowIndex && rowIndex < cell.rowIndex + cell.rowSpan
                    && cell.columnIndex <= boundary
                    && cell.columnIndex + cell.columnSpan > boundary + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCellCrossingRowBoundary(int rowIndex, int columnIndex) {
        for (var row : rows) {
            for (var cell : row.getChildren()) {
                if (cell.rowIndex <= rowIndex && cell.rowIndex + cell.rowSpan > rowIndex + 1
                    && columnIndex >= cell.columnIndex
                    && columnIndex < cell.columnIndex + cell.columnSpan) {
                    return true;
                }
            }
        }
        return false;
    }

    public LytTableColumn getOrCreateColumn(int index) {
        while (index >= columns.size()) {
            columns.add(new LytTableColumn());
        }
        return columns.get(index);
    }

    public int indexOfColumn(LytTableColumn column) {
        int index = columns.indexOf(column);
        return Math.max(0, index);
    }

    private void layoutColumns(int x, int availableWidth) {
        int innerWidth = Math
            .max(0, availableWidth - getOuterInset() * 2 - Math.max(0, columns.size() - 1) * cellSpacing);
        int totalPreferredWidth = 0;
        int totalPreferredPercent = 0;
        int flexibleColumns = 0;
        for (var column : columns) {
            if (column.preferredWidth > 0) {
                totalPreferredWidth += column.preferredWidth;
            } else if (column.preferredWidthPercent > 0) {
                totalPreferredPercent += column.preferredWidthPercent;
            } else {
                flexibleColumns++;
            }
        }

        int colX = x + getOuterInset();
        if (totalPreferredWidth > 0 || totalPreferredPercent > 0) {
            int preferredWidth = Math.min(totalPreferredWidth, innerWidth);
            int percentWidth = Math.max(0, innerWidth - preferredWidth);
            int remainingWidth = Math
                .max(0, percentWidth - Math.min(percentWidth, percentWidth * totalPreferredPercent / 100));
            int flexibleWidth = flexibleColumns > 0 ? remainingWidth / flexibleColumns : 0;
            int assignedWidth = 0;
            for (var column : columns) {
                column.x = colX;
                if (column.preferredWidth > 0) {
                    column.width = totalPreferredWidth > innerWidth
                        ? (int) ((long) column.preferredWidth * innerWidth / totalPreferredWidth)
                        : column.preferredWidth;
                } else if (column.preferredWidthPercent > 0) {
                    column.width = (int) ((long) percentWidth * column.preferredWidthPercent
                        / Math.max(100, totalPreferredPercent));
                } else {
                    column.width = flexibleWidth;
                }
                assignedWidth += column.width;
                colX += column.width + getColumnGap();
            }

            if (assignedWidth < innerWidth && flexibleColumns > 0) {
                var lastCol = columns.getLast();
                lastCol.width += innerWidth - assignedWidth;
            } else if (assignedWidth < innerWidth && !columns.isEmpty()) {
                columns.getLast().width += innerWidth - assignedWidth;
            }
            return;
        }

        int cellWidth = columns.isEmpty() ? 0 : innerWidth / columns.size();
        for (var column : columns) {
            column.x = colX;
            column.width = cellWidth;
            colX += column.width + getColumnGap();
        }

        var lastCol = columns.getLast();
        lastCol.width = Math.max(0, (x + availableWidth) - getOuterInset() - lastCol.x);
    }

    @Override
    public List<LytTableRow> getChildren() {
        return rows;
    }
}
