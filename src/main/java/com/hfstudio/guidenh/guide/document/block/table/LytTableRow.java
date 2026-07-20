package com.hfstudio.guidenh.guide.document.block.table;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * A row in {@link LytTable}. Contains {@link LytTableCell}.
 * <p>
 * A real block (not an eliminated wrapper): the Rust layout lays rows out as
 * flex Row containers — row height follows the tallest cell's content, so
 * wrapped text can no longer overflow a Java-pinned height.
 */
public class LytTableRow extends LytBlock {

    private final LytTable table;
    private final List<LytTableCell> cells = new ArrayList<>();

    public LytTableRow(LytTable table) {
        this.table = table;
        this.parent = table;
    }

    public LytTableCell appendCell() {
        var cell = new LytTableCell(table, this, table.getOrCreateColumn(cells.size()));
        cell.setMarginLeft(LytTable.CELL_BORDER);
        if (!cells.isEmpty()) {
            // The closing 1px border moves to the new last cell.
            cells.getLast()
                .setMarginRight(0);
        }
        cell.setMarginRight(LytTable.CELL_BORDER);
        cells.add(cell);
        return cell;
    }

    @Override
    public boolean usePrimitives() {
        // Rows have no visuals of their own (borders are the table's); the
        // collector descends to the cells. Without this the whole row would
        // fall back to a legacy HostDraw subtree.
        return true;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        var rowBottom = y;
        for (var cell : cells) {
            var column = cell.column;
            var cellBounds = cell.layout(context, column.x, y, column.width);
            rowBottom = Math.max(rowBottom, cellBounds.bottom());
        }
        return new LytRect(x, y, availableWidth, rowBottom - y);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var cell : cells) {
            cell.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public void render(RenderContext context) {
        for (var cell : cells) {
            cell.render(context);
        }
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
