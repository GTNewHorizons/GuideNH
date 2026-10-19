package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class TableCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("table");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytTable table = new LytTable();
        table.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);

        String alignStr = el.getAttributeString("align", "");
        List<Integer> widthHints = CsvTableCompiler.parseWidthHints(el.getAttributeString("widths", null));
        String tableWidth = el.getAttributeString("width", "");
        int tableWidthPixels = parsePixelWidth(tableWidth);
        if (tableWidthPixels > 0) {
            table.setPreferredWidth(tableWidthPixels);
        } else {
            int tableWidthPercent = parsePercent(tableWidth);
            if (tableWidthPercent > 0) {
                table.setPreferredWidthPercent(tableWidthPercent);
            }
        }
        if (!alignStr.contains(",")) {
            table.setHorizontalAlignment(alignStr);
        }
        String border = el.getAttributeString("border", "");
        if (isPixelValue(border)) {
            int borderWidth = parsePixelWidth(border);
            table.setBorderWidth(borderWidth);
            table.setOuterBorder(borderWidth > 0);
        }
        String cellSpacing = el.getAttributeString("cellspacing", "");
        if (isPixelValue(cellSpacing)) {
            table.setCellSpacing(parsePixelWidth(cellSpacing));
        }
        String cellPaddingValue = el.getAttributeString("cellpadding", "");
        int cellPadding = parsePixelWidth(cellPaddingValue);
        boolean hasCellPadding = isPixelValue(cellPaddingValue);
        List<String> columnWidths = new ArrayList<>();
        LytVBox captionedTable = null;

        boolean firstRow = !"false".equalsIgnoreCase(el.getAttributeString("header", "true"));
        for (var child : el.children()) {
            if (child instanceof MdxJsxFlowElement meta && tagEquals(meta, "table-meta")) {
                String content = meta.getAttributeString("content", "");
                if (!content.isEmpty()) {
                    List<Integer> metadataWidths = CsvTableCompiler.parseWidthHints(extractKramdownExpression(content));
                    if (!metadataWidths.isEmpty()) {
                        widthHints = metadataWidths;
                    }
                }
                continue;
            }
            if (child instanceof MdxJsxFlowElement caption && tagEquals(caption, "caption")) {
                if (captionedTable == null) {
                    captionedTable = new LytVBox();
                    captionedTable.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
                    table.setMarginBottom(0);
                }
                LytParagraph title = new LytParagraph();
                String captionAlign = caption.getAttributeString("align", "center")
                    .trim();
                title.modifyStyle(
                    style -> style.bold(true)
                        .alignment(parseAlignment(captionAlign, TextAlignment.CENTER)));
                compiler.compileInlineFragment(caption.children(), title);
                if (!title.isEmpty()) {
                    captionedTable.append(title);
                }
                continue;
            }
            if (child instanceof MdxJsxFlowElement group && tagEquals(group, "colgroup")) {
                readColumnWidths(group, columnWidths);
                continue;
            }
            if (child instanceof MdxJsxFlowElement column && tagEquals(column, "col")) {
                appendColumnWidths(column, columnWidths);
                continue;
            }
            if (child instanceof MdxJsxFlowElement rowElement && tagEquals(rowElement, "tr")) {
                firstRow = compileRow(
                    compiler,
                    table,
                    rowElement,
                    alignStr,
                    firstRow,
                    cellPadding,
                    hasCellPadding,
                    false);
            } else if (child instanceof MdxJsxFlowElement section
                && (tagEquals(section, "thead") || tagEquals(section, "tbody") || tagEquals(section, "tfoot"))) {
                    for (var rowChild : section.children()) {
                        if (rowChild instanceof MdxJsxFlowElement rowElement && tagEquals(rowElement, "tr")) {
                            firstRow = compileRow(
                                compiler,
                                table,
                                rowElement,
                                alignStr,
                                firstRow,
                                cellPadding,
                                hasCellPadding,
                                tagEquals(section, "thead"));
                        }
                    }
                }
        }
        applyColumnWidths(table, columnWidths);
        applyWidths(table, widthHints);
        if (captionedTable == null) {
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(table));
        } else {
            captionedTable.append(table);
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(captionedTable));
        }
    }

    private static boolean compileRow(PageCompiler compiler, LytTable table, MdxJsxFlowElement rowElement,
        String alignStr, boolean firstRow, int cellPadding, boolean hasCellPadding, boolean headerSection) {
        LytTableRow row = table.appendRow();
        if (firstRow && !headerSection
            && rowElement.children()
                .stream()
                .noneMatch(child -> child instanceof MdxJsxFlowElement cell && tagEquals(cell, "th"))) {
            row.modifyStyle(style -> style.bold(true));
        }
        String rowAlign = rowElement.getAttributeString("align", "")
            .trim();
        String rowVAlign = rowElement.getAttributeString("valign", "")
            .trim();
        for (var cellChild : rowElement.children()) {
            if (cellChild instanceof MdxJsxFlowElement cellElement
                && (tagEquals(cellElement, "td") || tagEquals(cellElement, "th"))) {
                int columnSpan = parseSpan(cellElement.getAttributeString("colspan", "1"));
                int rowSpan = parseSpan(cellElement.getAttributeString("rowspan", "1"));
                LytTableCell cell = row.appendCell(columnSpan, rowSpan);
                if (hasCellPadding) {
                    cell.setPadding(cellPadding);
                }
                if (tagEquals(cellElement, "th")) {
                    cell.modifyStyle(style -> style.bold(true));
                }
                String cellAlign = cellElement.getAttributeString("align", "")
                    .trim();
                if (cellAlign.isEmpty() && !rowAlign.isEmpty()) {
                    cellAlign = rowAlign;
                }
                if (cellAlign.isEmpty() && alignStr.contains(",")) {
                    String[] parts = alignStr.split(",");
                    if (cell.getColumnIndex() < parts.length) {
                        cellAlign = parts[cell.getColumnIndex()].trim();
                    }
                }
                switch (cellAlign.toLowerCase(Locale.ROOT)) {
                    case "center" -> cell.modifyStyle(style -> style.alignment(TextAlignment.CENTER));
                    case "right" -> cell.modifyStyle(style -> style.alignment(TextAlignment.RIGHT));
                }
                String cellVAlign = cellElement.getAttributeString("valign", "")
                    .trim();
                cell.setVerticalAlignment(cellVAlign.isEmpty() ? rowVAlign : cellVAlign);
                applyCellStyle(cell, cellElement);
                compiler.compileTableCellContent(cellElement.children(), cell);
            }
        }
        return false;
    }

    private static void applyCellStyle(LytTableCell cell, MdxJsxElementFields element) {
        String color = element.getAttributeString("color", null);
        String background = element.getAttributeString("bgcolor", null);
        if (background == null) {
            background = element.getAttributeString("background", null);
        }
        String style = element.getAttributeString("style", null);
        if (style != null && style.length() <= 4096) {
            for (String declaration : style.split(";")) {
                int separator = declaration.indexOf(':');
                if (separator <= 0) {
                    continue;
                }
                String key = declaration.substring(0, separator)
                    .trim()
                    .toLowerCase(Locale.ROOT);
                String value = declaration.substring(separator + 1)
                    .trim();
                if (value.length() > 512 || value.indexOf('<') >= 0 || value.indexOf('>') >= 0) {
                    continue;
                }
                switch (key) {
                    case "color" -> color = value;
                    case "background", "background-color" -> background = value;
                    case "font-weight" -> {
                        if ("bold".equalsIgnoreCase(value) || "700".equals(value)) {
                            cell.modifyStyle(textStyle -> textStyle.bold(true));
                        }
                    }
                    case "text-decoration" -> {
                        if (value.toLowerCase(Locale.ROOT)
                            .contains("underline")) {
                            cell.modifyStyle(textStyle -> textStyle.underlined(true));
                        }
                        if (value.toLowerCase(Locale.ROOT)
                            .contains("line-through")) {
                            cell.modifyStyle(textStyle -> textStyle.strikethrough(true));
                        }
                    }
                    case "text-align" -> cell
                        .modifyStyle(textStyle -> textStyle.alignment(parseAlignment(value, null)));
                    default -> {}
                }
            }
        }
        if (color != null) {
            String resolvedColor = color;
            cell.modifyStyle(
                textStyle -> textStyle
                    .color(new ConstantColor(parseColor(resolvedColor, ColorUtils.WHITE.getColor()))));
        }
        if (background != null) {
            String resolvedBackground = background;
            cell.setCellBackground(
                new ConstantColor(parseColor(resolvedBackground, ColorUtils.TRANSPARENT.getColor())));
        }
    }

    private static int parseColor(String value, int fallback) {
        return ChartAttrParser.parseColor(value, fallback);
    }

    private static void applyWidths(LytTable table, List<Integer> widths) {
        var columns = table.getColumns();
        for (int index = 0; index < widths.size() && index < columns.size(); index++) {
            columns.get(index)
                .setPreferredWidth(widths.get(index));
        }
    }

    private static void applyColumnWidths(LytTable table, List<String> widths) {
        var columns = table.getColumns();
        for (int index = 0; index < widths.size() && index < columns.size(); index++) {
            String value = widths.get(index);
            if (value.endsWith("%")) {
                try {
                    columns.get(index)
                        .setPreferredWidthPercent(Integer.parseInt(value.substring(0, value.length() - 1)));
                } catch (NumberFormatException ignored) {
                    columns.get(index)
                        .setPreferredWidth(0);
                }
            } else {
                columns.get(index)
                    .setPreferredWidth(parsePixelWidth(value));
            }
        }
    }

    private static void readColumnWidths(MdxJsxFlowElement group, List<String> widths) {
        int start = widths.size();
        for (var child : group.children()) {
            if (child instanceof MdxJsxFlowElement column && tagEquals(column, "col")) {
                appendColumnWidths(column, widths);
            }
        }
        if (widths.size() == start) {
            appendColumnWidths(group, widths);
        }
    }

    private static void appendColumnWidths(MdxJsxFlowElement column, List<String> widths) {
        String widthValue = column.getAttributeString("width", "")
            .trim();
        String width = normalizeColumnWidth(widthValue);
        int span = parseSpan(column.getAttributeString("span", "1"));
        for (int index = 0; index < span && widths.size() < 1024; index++) {
            widths.add(width);
        }
    }

    private static String normalizeColumnWidth(String value) {
        if (value.matches("\\d+(?:px)?")) {
            return value.replaceFirst("(?i)px$", "");
        }
        if (value.matches("\\d+(?:\\.\\d+)?%")) {
            return value;
        }
        return "";
    }

    private static int parsePixelWidth(String value) {
        String pixels = value.trim()
            .replaceFirst("(?i)px$", "")
            .trim();
        try {
            return Math.max(0, Math.min(10000, Integer.parseInt(pixels)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isPixelValue(String value) {
        return value != null && value.trim()
            .matches("\\d+(?:px)?");
    }

    private static int parsePercent(String value) {
        String percent = value.trim()
            .replace("%", "")
            .trim();
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(percent)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static TextAlignment parseAlignment(String value, TextAlignment fallback) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "left" -> TextAlignment.LEFT;
            case "right" -> TextAlignment.RIGHT;
            case "center" -> TextAlignment.CENTER;
            default -> fallback;
        };
    }

    private static boolean tagEquals(MdxJsxFlowElement element, String expected) {
        return expected.equalsIgnoreCase(element.name());
    }

    private static int parseSpan(String value) {
        try {
            return Math.max(1, Math.min(1024, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String extractKramdownExpression(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start + 1, end)
                .trim();
        }
        return "";
    }
}
