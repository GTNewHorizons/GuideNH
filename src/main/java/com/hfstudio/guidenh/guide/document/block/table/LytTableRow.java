package com.hfstudio.guidenh.guide.document.block.table;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * A row in {@link LytTable}. Contains {@link LytTableCell}.
 */
public class LytTableRow extends LytNode {

    private final LytTable table;
    private final List<LytTableCell> cells = new ArrayList<>();
    LytRect bounds = LytRect.empty();
    private int nextColumn;
    final int rowIndex;

    public LytTableRow(LytTable table) {
        this(
            table,
            table.getChildren()
                .size());
    }

    public LytTableRow(LytTable table, int rowIndex) {
        this.table = table;
        this.rowIndex = rowIndex;
        this.parent = table;
    }

    @Override
    public LytRect getBounds() {
        return bounds;
    }

    public LytTableCell appendCell() {
        return appendCell(1, 1);
    }

    public LytTableCell appendCell(int columnSpan, int rowSpan) {
        int safeColumnSpan = Math.max(1, columnSpan);
        int safeRowSpan = Math.max(1, rowSpan);
        int columnIndex = nextColumn;
        while (table.isOccupied(rowIndex, columnIndex, safeColumnSpan)) {
            columnIndex++;
        }
        var cell = new LytTableCell(table, this, rowIndex, columnIndex, safeColumnSpan, safeRowSpan);
        cells.add(cell);
        nextColumn = columnIndex + safeColumnSpan;
        for (int index = columnIndex; index < nextColumn; index++) {
            table.getOrCreateColumn(index);
        }
        return cell;
    }

    @Override
    public List<LytTableCell> getChildren() {
        return cells;
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        if (!(newChild instanceof LytTableCell)) return;
        int idx = cells.indexOf(oldChild);
        if (idx < 0) return;
        cells.set(idx, (LytTableCell) newChild);
    }
}
