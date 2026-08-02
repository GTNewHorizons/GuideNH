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

    /**
     * Thickness of the horizontal separator drawn below the header row.
     * Deliberately thicker than the 1px data-row separators so the header row
     * is visually distinct.
     */
    public static final int HEADER_SEPARATOR_THICKNESS = 2;
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
        // Column border lines (vertical lines between columns). X comes from the
        // Rust-written cell bounds of the row with the most cells (F3 — Java
        // must not compute geometry). column.x/column.width are x=0
        // serialization-time declarations and must not drive drawing.
        var sourceRow = widestRow();
        for (int i = 0; i < columns.size() - 1; i++) {
            c.emit(
                new GuideRenderPrimitive.FillRect(
                    columnSeparatorX(sourceRow, i),
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
                    row.isHeader() ? HEADER_SEPARATOR_THICKNESS : 1,
                    SymbolicColor.TABLE_BORDER.resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current())));
        }
        // Cells are children — collectFrom traversal handles them
    }

    @Override
    public void render(RenderContext context) {
        // Render the table cell borders
        var bounds = getBounds();
        var sourceRow = widestRow();
        for (int i = 0; i < columns.size() - 1; i++) {
            context.fillRect(columnSeparatorX(sourceRow, i), bounds.y(), 1, bounds.height(), SymbolicColor.TABLE_BORDER);
        }

        for (int i = 0; i < rows.size() - 1; i++) {
            var row = rows.get(i);
            context.fillRect(
                bounds.x(),
                row.getBounds()
                    .bottom(),
                bounds.width(),
                row.isHeader() ? HEADER_SEPARATOR_THICKNESS : 1,
                SymbolicColor.TABLE_BORDER);
        }

        for (var row : rows) {
            row.render(context);
        }
    }

    /**
     * The row with the most cells (first row wins ties). Its Rust-written cell
     * bounds are the source for vertical separator positions — all rows share
     * the table's column x-structure, so one row's cell boundaries stand in
     * for every row. {@code null} when the table has no rows (then no
     * vertical separators can be derived; column model is the fallback).
     */
    private LytTableRow widestRow() {
        LytTableRow widest = null;
        for (var row : rows) {
            if (widest == null || row.getChildren().size() > widest.getChildren().size()) {
                widest = row;
            }
        }
        return widest;
    }

    /**
     * Document-space x of the vertical separator between column {@code i} and
     * {@code i + 1}, derived from the Rust-written cell bounds of
     * {@code sourceRow} (F3 — the line positions must come from Rust layout
     * data, never from Java-computed column geometry).
     * <p>
     * Primary reference: {@code cell[i].getBounds().right()} — the left edge of
     * the 1px CELL_BORDER gutter between adjacent cells. This mirrors the
     * horizontal separators, which are drawn at {@code row.getBounds().bottom()}
     * (the top edge of the row gutter), so vertical and horizontal lines meet at
     * the cell corners. The alternative {@code cell[i+1].getBounds().x()} is the
     * gutter's right edge — exactly 1px right of {@code right()} while the gutter
     * is CELL_BORDER wide — and their integer midpoint degenerates to the left
     * edge, so {@code right()} is the stable choice. When cell {@code i}'s
     * bounds are missing, the boundary is recovered from the right neighbour's
     * {@code cell[i+1].x() - CELL_BORDER}.
     * <p>
     * Fallback: when the widest row has no usable Rust bounds for this boundary
     * (no rows, fewer cells than columns, or bounds not written back), the
     * legacy column-model position {@code column.x + column.width} is used. This
     * only guards degenerate / pre-layout paths — after a Rust layout pass every
     * flat node (cells included) receives a written-back rect.
     */
    private int columnSeparatorX(LytTableRow sourceRow, int i) {
        if (sourceRow != null && i + 1 < sourceRow.getChildren().size()) {
            var cells = sourceRow.getChildren();
            LytRect left = cells.get(i).getBounds();
            LytRect right = cells.get(i + 1).getBounds();
            if (!left.isEmpty()) {
                return left.right();
            }
            if (!right.isEmpty()) {
                return right.x() - CELL_BORDER;
            }
        }
        return columns.get(i).x + columns.get(i).width;
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

            // Only distribute remainder to flexible (undeclared) columns.
            // When all columns have declared widths, the table stays at the
            // sum of declared widths (natural width) — the last column must
            // NOT absorb the leftover space (R4-4 fix).
            if (flexibleColumns > 0 && assignedWidth < innerWidth) {
                int leftover = innerWidth - assignedWidth;
                var lastCol = columns.getLast();
                if (lastCol.preferredWidth == 0) {
                    lastCol.width += leftover;
                }
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
