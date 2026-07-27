package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * A block that renders a file tree as a stack of rows where each row carries a configurable depth
 * of connector lines drawn directly via {@link #computePrimitives}, an optional icon block and a
 * {@link LytParagraph} payload re-parsed from inline markdown.
 *
 * <p>Each row is wrapped in a {@link LytHBox} row container whose {@code marginLeft} encodes the
 * indentation level ({@code slots.size() * indentPx}) and whose children are the optional icon
 * block followed by the payload paragraph. The row containers are full tree children
 * ({@link #getChildren()}), so they participate in Rust layout — the paragraphs receive proper
 * glyph data and bounds computed by the Rust layout engine. Connector lines are still drawn in
 * {@link #computePrimitives} using the Rust-computed row container bounds for Y positions.
 */
public class LytFileTree extends LytBlock {

    private static final int DEFAULT_INDENT_PX = 14;
    private static final int DEFAULT_ROW_GAP_PX = 0;
    private static final int DEFAULT_ICON_BOX_PX = 16;
    private static final int DEFAULT_ICON_GAP_PX = 4;
    private static final int CONNECTOR_THICKNESS = 1;

    private final List<Row> rows = new ArrayList<>();
    private final List<LytHBox> rowContainers = new ArrayList<>();
    @Getter
    private int indentPx = DEFAULT_INDENT_PX;
    @Getter
    private int rowGapPx = DEFAULT_ROW_GAP_PX;
    private int iconBoxPx = DEFAULT_ICON_BOX_PX;
    private int iconGapPx = DEFAULT_ICON_GAP_PX;

    public void appendRow(List<SlotKind> slots, @Nullable LytBlock iconBlock, LytParagraph payload) {
        LytHBox container = new LytHBox();
        container.setWrap(false);
        container.setGap(iconGapPx);
        container.setAlignItems(AlignItems.CENTER);
        if (iconBlock != null) {
            container.append(iconBlock);
        }
        container.append(payload);
        // Indentation as margin-left on the row container (previously set in
        // computeLayout — moved here so the margin is available for serialization
        // even after the Java layout pre-pass is removed).
        int marginLeft = slots.size() * indentPx;
        container.setMarginLeft(marginLeft);
        // Gap between rows as margin-bottom (last-row gap cleared by
        // finalizeRowGaps).
        container.setMarginBottom(rowGapPx);
        rows.add(new Row(new ArrayList<>(slots), container));
        rowContainers.add(container);
    }

    /**
     * Clear the bottom margin on the last row so no trailing gap is added.
     * Call after all rows have been appended.
     */
    public void finalizeRowGaps() {
        if (!rowContainers.isEmpty()) {
            rowContainers.get(rowContainers.size() - 1).setMarginBottom(0);
        }
    }

    public void setIndentPx(int indentPx) {
        if (indentPx > 0) {
            this.indentPx = indentPx;
        }
    }

    public void setRowGapPx(int rowGapPx) {
        this.rowGapPx = Math.max(0, rowGapPx);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return rowContainers;
    }

    @Override
    public void removeChild(LytNode node) {
        for (int i = 0; i < rowContainers.size(); i++) {
            if (rowContainers.get(i) == node) {
                rowContainers.get(i).parent = null;
                rowContainers.remove(i);
                rows.remove(i);
                return;
            }
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Margins are now set in appendRow / finalizeRowGaps.
        // Children are laid out by the Rust layout engine — this method
        // is retained for compatibility but is no longer called by the
        // document pipeline (the Java layout pre-pass has been removed).
        // If called directly, lay out children minimally.
        int currentY = y;
        int totalHeight = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            LytHBox container = row.container;
            int marginLeft = container.getMarginLeft();
            container.layout(context, x + marginLeft, currentY, availableWidth - marginLeft);
            LytRect rowBounds = container.getBounds();
            int rowHeight = Math.max(1, rowBounds.height());
            totalHeight += rowHeight;
            currentY += rowHeight + container.getMarginBottom();
        }
        return new LytRect(x, y, availableWidth, totalHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (LytHBox row : rowContainers) {
            row.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        int baseX = bounds.x();
        int connectorColor = SymbolicColor.TABLE_BORDER
            .resolve(com.hfstudio.guidenh.guide.color.LightDarkMode.current());
        int halfIndent = indentPx / 2;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            LytRect rowBounds = row.container.getBounds();
            int rowY = rowBounds.y();
            int rowHeight = rowBounds.height();
            int rowMidY = rowY + Math.max(0, rowHeight - CONNECTOR_THICKNESS) / 2;
            // Extend vertical connector to the bottom of the margin-box gap.
            int rowBottomY = rowY + rowHeight + row.container.getMarginBottom();
            int slotCount = row.slots.size();
            int columnCenterX = baseX + halfIndent;
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++, columnCenterX += indentPx) {
                SlotKind slot = row.slots.get(slotIndex);
                switch (slot) {
                    case VERTICAL -> emitVerticalLine(c, columnCenterX, rowY, rowBottomY, connectorColor);
                    case BRANCH -> {
                        emitVerticalLine(c, columnCenterX, rowY, rowBottomY, connectorColor);
                        emitHorizontalLine(
                            c,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case LAST_BRANCH -> {
                        emitVerticalLine(c, columnCenterX, rowY, rowMidY + CONNECTOR_THICKNESS, connectorColor);
                        emitHorizontalLine(
                            c,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case EMPTY -> {
                        // Empty slot draws nothing.
                    }
                }
            }
        }
    }

    private static void emitVerticalLine(PrimitiveCollector c, int x, int yStart, int yEnd, int color) {
        int top = Math.min(yStart, yEnd);
        int height = Math.abs(yEnd - yStart);
        if (height <= 0) {
            return;
        }
        c.emit(new GuideRenderPrimitive.FillRect(x, top, CONNECTOR_THICKNESS, height, color));
    }

    private static void emitHorizontalLine(PrimitiveCollector c, int xStart, int xEnd, int y, int color) {
        int left = Math.min(xStart, xEnd);
        int width = Math.abs(xEnd - xStart);
        if (width <= 0) {
            return;
        }
        c.emit(new GuideRenderPrimitive.FillRect(left, y, width, CONNECTOR_THICKNESS, color));
    }

    @Override
    public void render(RenderContext context) {
        renderConnectors(context);
        for (LytHBox row : rowContainers) {
            row.render(context);
        }
    }

    private void renderConnectors(RenderContext context) {
        int baseX = bounds.x();
        // Resolve symbolic color once per frame instead of on every fillRect.
        int connectorColor = context.resolveColor(SymbolicColor.TABLE_BORDER);
        int halfIndent = indentPx / 2;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            LytRect rowBounds = row.container.getBounds();
            int rowY = rowBounds.y();
            int rowHeight = rowBounds.height();
            int rowMidY = rowY + Math.max(0, rowHeight - CONNECTOR_THICKNESS) / 2;
            int rowBottomY = rowY + rowHeight + row.container.getMarginBottom();
            int slotCount = row.slots.size();
            int columnCenterX = baseX + halfIndent;
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++, columnCenterX += indentPx) {
                SlotKind slot = row.slots.get(slotIndex);
                switch (slot) {
                    case VERTICAL -> drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                    case BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                        drawHorizontalLine(
                            context,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case LAST_BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowMidY + CONNECTOR_THICKNESS, connectorColor);
                        drawHorizontalLine(
                            context,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case EMPTY -> {
                        // Empty slot draws nothing.
                    }
                }
            }
        }
    }

    private static void drawVerticalLine(RenderContext context, int x, int yStart, int yEnd, int color) {
        int top = Math.min(yStart, yEnd);
        int height = Math.abs(yEnd - yStart);
        if (height <= 0) {
            return;
        }
        context.fillRect(new LytRect(x, top, CONNECTOR_THICKNESS, height), color);
    }

    private static void drawHorizontalLine(RenderContext context, int xStart, int xEnd, int y, int color) {
        int left = Math.min(xStart, xEnd);
        int width = Math.abs(xEnd - xStart);
        if (width <= 0) {
            return;
        }
        context.fillRect(new LytRect(left, y, width, CONNECTOR_THICKNESS), color);
    }

    private static class Row {

        final List<SlotKind> slots;
        final LytHBox container;

        Row(List<SlotKind> slots, LytHBox container) {
            this.slots = slots;
            this.container = container;
        }
    }
}
