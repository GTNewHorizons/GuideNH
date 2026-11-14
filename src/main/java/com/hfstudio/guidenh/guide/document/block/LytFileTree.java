package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * A block that renders a file tree as a stack of rows where each row carries a configurable depth
 * of connector lines drawn directly with {@link RenderContext#fillRect}, an optional icon block
 * and a {@link LytParagraph} payload re-parsed from inline markdown.
 *
 * <p>
 * Connectors are derived strictly from the slot kinds parsed for each row, so the visual output is
 * deterministic with respect to the source.
 */
public class LytFileTree extends LytBlock {

    private static final int DEFAULT_INDENT_PX = 14;
    private static final int DEFAULT_ROW_GAP_PX = 0;
    private static final int DEFAULT_ICON_BOX_PX = 16;
    private static final int DEFAULT_ICON_GAP_PX = 4;
    private static final int CONNECTOR_THICKNESS = 1;

    private final List<Row> rows = new ArrayList<>();
    private final List<LytNode> childNodes = new ArrayList<>();
    @Getter
    private int indentPx = DEFAULT_INDENT_PX;
    @Getter
    private int rowGapPx = DEFAULT_ROW_GAP_PX;
    private int iconBoxPx = DEFAULT_ICON_BOX_PX;
    private int iconGapPx = DEFAULT_ICON_GAP_PX;
    private float layoutIndentPx = DEFAULT_INDENT_PX;

    public void appendRow(List<SlotKind> slots, @Nullable LytBlock iconBlock, LytParagraph payload) {
        Row row = new Row(new ArrayList<>(slots), iconBlock, payload);
        if (iconBlock != null) {
            iconBlock.parent = this;
            childNodes.add(iconBlock);
        }
        payload.parent = this;
        childNodes.add(payload);
        rows.add(row);
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

    public List<Row> getRows() {
        return List.copyOf(rows);
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return childNodes;
    }

    @Override
    public void removeChild(LytNode node) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            if (row.payload == node) {
                row.payload.parent = null;
                childNodes.remove(row.payload);
                if (row.iconBlock != null) {
                    row.iconBlock.parent = null;
                    childNodes.remove(row.iconBlock);
                }
                rows.remove(rowIndex);
                return;
            }
            if (row.iconBlock == node) {
                row.iconBlock.parent = null;
                childNodes.remove(row.iconBlock);
                rows.set(rowIndex, new Row(row.slots, null, row.payload));
                return;
            }
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int currentY = y;
        int totalHeight = 0;
        int maxDepth = rows.stream()
            .mapToInt(row -> row.slots.size())
            .max()
            .orElse(0);
        int iconReserve = rows.stream()
            .anyMatch(row -> row.iconBlock != null) ? iconBoxPx + iconGapPx : 0;
        int textReserve = Math.min(80, availableWidth / 2);
        layoutIndentPx = maxDepth == 0 ? indentPx
            : Math.min(indentPx, Math.max(0, availableWidth - textReserve - iconReserve) / (float) maxDepth);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int iconX = x + Math.round(row.slots.size() * layoutIndentPx);
            int payloadX;
            int iconHeight = 0;
            if (row.iconBlock != null) {
                // Measure the icon while keeping readable space for the filename.
                int iconAvailable = Math.max(1, x + availableWidth - iconX - textReserve - iconGapPx);
                row.iconBlock.layout(context, iconX, currentY, Math.max(1, iconAvailable));
                LytRect iconBounds = row.iconBlock.getBounds();
                int actualIconWidth = iconBounds.width();
                int reservedIconWidth = Math.max(iconBoxPx, actualIconWidth);
                iconHeight = iconBounds.height();
                payloadX = iconX + reservedIconWidth + iconGapPx;
            } else {
                payloadX = iconX;
            }
            int payloadAvailable = Math.max(1, x + availableWidth - payloadX);
            LytRect payloadBounds = row.payload.layout(context, payloadX, currentY, payloadAvailable);
            LytRect firstLine = row.payload.getFirstLineBounds();
            int firstLineHeight = firstLine != null ? firstLine.height() : payloadBounds.height();
            int firstRowHeight = Math.max(firstLineHeight, iconHeight);
            centerRowChild(row.payload, firstRowHeight, firstLineHeight);
            if (row.iconBlock != null) {
                centerRowChild(row.iconBlock, firstRowHeight, iconHeight);
            }
            int rowHeight = Math.max(
                row.payload.getBounds()
                    .bottom() - currentY,
                iconHeight);
            if (rowHeight <= 0) {
                rowHeight = 1;
            }
            row.rowY = currentY;
            row.rowHeight = rowHeight;
            row.connectorY = currentY + Math.max(0, firstRowHeight - CONNECTOR_THICKNESS) / 2;
            currentY += rowHeight;
            totalHeight += rowHeight;
            if (i < rows.size() - 1) {
                currentY += rowGapPx;
                totalHeight += rowGapPx;
            }
        }

        return new LytRect(x, y, availableWidth, totalHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (Row row : rows) {
            row.rowY += deltaY;
            row.connectorY += deltaY;
            if (row.iconBlock != null) {
                row.iconBlock.moveLayoutPos(deltaX, deltaY);
            }
            row.payload.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public void render(RenderContext context) {
        renderConnectors(context);
        for (Row row : rows) {
            if (row.iconBlock != null) {
                row.iconBlock.render(context);
            }
            row.payload.render(context);
        }
    }

    private void renderConnectors(RenderContext context) {
        int baseX = bounds.x();
        // Resolve symbolic color once per frame instead of on every fillRect.
        int connectorColor = context.resolveColor(ColorUtils.TABLE_BORDER);
        for (Row row : rows) {
            int rowY = row.rowY;
            int rowHeight = row.rowHeight;
            int rowMidY = row.connectorY;
            int rowBottomY = rowY + rowHeight + rowGapPx;
            int slotCount = row.slots.size();
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                int columnCenterX = baseX + Math.round((slotIndex + 0.5f) * layoutIndentPx);
                int columnEndX = baseX + Math.round((slotIndex + 1) * layoutIndentPx);
                SlotKind slot = row.slots.get(slotIndex);
                switch (slot) {
                    case VERTICAL -> drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                    case BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                        drawHorizontalLine(context, columnCenterX, columnEndX, rowMidY, connectorColor);
                    }
                    case LAST_BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowMidY + CONNECTOR_THICKNESS, connectorColor);
                        drawHorizontalLine(context, columnCenterX, columnEndX, rowMidY, connectorColor);
                    }
                    case EMPTY -> {
                        // Empty slot draws nothing.
                    }
                }
            }
        }
    }

    private void centerRowChild(LytBlock child, int rowHeight, int childHeight) {
        if (childHeight <= 0 || childHeight >= rowHeight) {
            return;
        }
        child.moveLayoutPos(0, (rowHeight - childHeight) / 2);
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

    public static class Row {

        private final List<SlotKind> slots;
        @Nullable
        private final LytBlock iconBlock;
        private final LytParagraph payload;
        private int rowY;
        private int rowHeight;
        private int connectorY;

        Row(List<SlotKind> slots, @Nullable LytBlock iconBlock, LytParagraph payload) {
            this.slots = slots;
            this.iconBlock = iconBlock;
            this.payload = payload;
        }

        public List<SlotKind> getSlots() {
            return slots;
        }

        @Nullable
        public LytBlock getIconBlock() {
            return iconBlock;
        }

        public LytParagraph getPayload() {
            return payload;
        }
    }
}
