package com.hfstudio.guidenh.guide.document.block.table;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytTable extends LytBlock {

    /**
     * Width of border around cells.
     */
    public static final int CELL_BORDER = 1;
    private final List<LytTableRow> rows = new ArrayList<>();

    @Getter
    private final List<LytTableColumn> columns = new ArrayList<>();

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (columns.isEmpty()) {
            return LytRect.empty();
        }

        layoutColumns(x, availableWidth);

        // Layout each row (rows lay out their own cells against the column model)
        var currentY = y + CELL_BORDER;
        for (var row : rows) {
            var rowBounds = row.layout(context, x, currentY, availableWidth);
            currentY = rowBounds.bottom() + CELL_BORDER;
        }

        return new LytRect(x, y, availableWidth, currentY - y);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var col : columns) {
            col.x += deltaX;
        }
        for (var row : rows) {
            row.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        var bounds = getBounds();
        // Column border lines (vertical lines between columns)
        for (int i = 0; i < columns.size() - 1; i++) {
            var column = columns.get(i);
            var colRight = column.x + column.width;
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    colRight,
                    bounds.y(),
                    1,
                    bounds.height(),
                    SymbolicColor.TABLE_BORDER.resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current())));
        }
        // Row border lines (horizontal lines between rows)
        for (int i = 0; i < rows.size() - 1; i++) {
            var row = rows.get(i);
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    bounds.x(),
                    row.getBounds()
                        .bottom(),
                    bounds.width(),
                    1,
                    SymbolicColor.TABLE_BORDER.resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current())));
        }
        // Cells are children — collectFrom traversal handles them
    }

    @Override
    public void render(RenderContext context) {
        // Render the table cell borders
        var bounds = getBounds();
        for (int i = 0; i < columns.size() - 1; i++) {
            var column = columns.get(i);
            var colRight = column.x + column.width;
            context.fillRect(colRight, bounds.y(), 1, bounds.height(), SymbolicColor.TABLE_BORDER);
        }

        for (int i = 0; i < rows.size() - 1; i++) {
            var row = rows.get(i);
            context.fillRect(
                bounds.x(),
                row.getBounds()
                    .bottom(),
                bounds.width(),
                1,
                SymbolicColor.TABLE_BORDER);
        }

        for (var row : rows) {
            row.render(context);
        }
    }

    public LytTableRow appendRow() {
        var row = new LytTableRow(this);
        if (rows.isEmpty()) {
            row.setMarginTop(CELL_BORDER);
        }
        row.setMarginBottom(CELL_BORDER);
        rows.add(row);
        return row;
    }

    public LytTableColumn getOrCreateColumn(int index) {
        while (index >= columns.size()) {
            columns.add(new LytTableColumn());
        }
        return columns.get(index);
    }

    /**
     * Distribute available width among columns. Called by the serializer
     * (no longer by the Java pre-pass) so column widths are set before
     * serialization. {@code x} is the table's left edge in document coords.
     */
    public void layoutColumns(int x, int availableWidth) {
        if (columns.isEmpty()) return;
        int innerWidth = Math.max(0, availableWidth - (columns.size() + 1) * CELL_BORDER);
        int totalPreferredWidth = 0;
        int flexibleColumns = 0;
        for (var column : columns) {
            if (column.preferredWidth > 0) {
                totalPreferredWidth += column.preferredWidth;
            } else {
                flexibleColumns++;
            }
        }

        int colX = x + CELL_BORDER;
        if (totalPreferredWidth > 0 && totalPreferredWidth <= innerWidth) {
            int remainingWidth = innerWidth - totalPreferredWidth;
            int flexibleWidth = flexibleColumns > 0 ? remainingWidth / flexibleColumns : 0;
            int assignedWidth = 0;
            for (var column : columns) {
                column.x = colX;
                column.width = column.preferredWidth > 0 ? column.preferredWidth : flexibleWidth;
                assignedWidth += column.width;
                colX += column.width + CELL_BORDER;
            }

            if (assignedWidth < innerWidth) {
                var lastCol = columns.getLast();
                lastCol.width += innerWidth - assignedWidth;
            }
            return;
        }

        int cellWidth = columns.isEmpty() ? 0 : innerWidth / columns.size();
        for (var column : columns) {
            column.x = colX;
            column.width = cellWidth;
            colX += column.width + CELL_BORDER;
        }

        var lastCol = columns.getLast();
        lastCol.width = (x + availableWidth) - lastCol.x - CELL_BORDER;
    }

    @Override
    public List<LytTableRow> getChildren() {
        return rows;
    }
}
