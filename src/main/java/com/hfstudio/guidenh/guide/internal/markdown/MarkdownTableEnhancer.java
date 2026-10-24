package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MarkdownTableEnhancer {

    private static final int MAX_TABLE_ROWS = 4096;
    private static final int MAX_TABLE_CELLS_PER_ROW = 2048;

    private static final Pattern ATTRIBUTE_DEFINITION = Pattern
        .compile("^\\s*\\{:\\s*([A-Za-z][A-Za-z0-9_-]*)\\s*:\\s*(.*?)\\s*}\\s*$");
    private static final Pattern INLINE_ATTRIBUTES = Pattern.compile("\\s*\\{:\\s*(.*?)\\s*}\\s*$");

    private MarkdownTableEnhancer() {}

    public static void enhance(MdAstRoot root, String source) {
        if (root == null) {
            return;
        }
        Map<String, String> definitions = collectDefinitions(source);
        normalizeTableMetadata(root, definitions);
        visit(root, source != null ? source : "", definitions);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void normalizeTableMetadata(MdAstParent<?> parent, Map<String, String> definitions) {
        List<MdAstAnyContent> children = (List) parent.children();
        for (int index = 0; index < children.size(); index++) {
            if (!(children.get(index) instanceof GfmTable table)) {
                continue;
            }
            if (index > 0) {
                MdAstAnyContent previous = children.get(index - 1);
                if (previous instanceof MdAstParagraph paragraph) {
                    String metadata = plainText(paragraph);
                    Map<String, String> attributes = parseMetadataLine(metadata, definitions);
                    if (isAttributeDefinition(metadata) || !attributes.isEmpty()) {
                        table.attributes.putAll(attributes);
                        children.remove(index - 1);
                        index--;
                    }
                }
            }
            if (index + 1 < children.size()) {
                MdAstAnyContent next = children.get(index + 1);
                if (next instanceof MdAstParagraph paragraph) {
                    String metadata = plainText(paragraph);
                    Map<String, String> attributes = parseMetadataLine(metadata, definitions);
                    if (isAttributeDefinition(metadata) || !attributes.isEmpty()) {
                        table.attributes.putAll(attributes);
                        children.remove(index + 1);
                    }
                }
            }
            normalizeTrailingMetadataRow(table, definitions);
        }
    }

    private static void normalizeTrailingMetadataRow(GfmTable table, Map<String, String> definitions) {
        List<GfmTableRow> rows = table.children();
        if (rows.isEmpty()) {
            return;
        }
        GfmTableRow row = rows.getLast();
        String rowText = getTableRowText(row);
        Map<String, String> attributes = parseMetadataLine(rowText, definitions);
        if (isAttributeDefinition(rowText) || !attributes.isEmpty()) {
            table.attributes.putAll(attributes);
            rows.removeLast();
        }
    }

    private static String getTableRowText(GfmTableRow row) {
        StringBuilder text = new StringBuilder();
        for (GfmTableCell cell : row.children()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(cell.toText());
        }
        return text.toString()
            .trim();
    }

    private static String plainText(MdAstParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (MdAstPhrasingContent child : paragraph.children()) {
            if (child instanceof MdAstText value) {
                text.append(value.value);
            }
        }
        return text.toString()
            .trim();
    }

    private static Map<String, String> parseMetadataLine(String text, Map<String, String> definitions) {
        if (text == null) {
            return Collections.emptyMap();
        }
        Matcher matcher = INLINE_ATTRIBUTES.matcher(text.trim());
        if (!matcher.matches()) {
            return Collections.emptyMap();
        }
        if (isAttributeDefinition(text)) {
            return Collections.emptyMap();
        }
        return parseAttributes(matcher.group(1), definitions, 0);
    }

    private static boolean isAttributeDefinition(String text) {
        return text != null && text.trim()
            .matches("^\\{:\\s*[A-Za-z][A-Za-z0-9_-]*\\s*:");
    }

    private static void visit(MdAstNode node, String source, Map<String, String> definitions) {
        if (node instanceof MdAstParent<?>parent) {
            normalizeTableMetadata(parent, definitions);
        }
        if (node instanceof GfmTable table) {
            enhanceTable(table, source, definitions);
        }
        if (node instanceof MdAstParent<?>parent) {
            for (Object child : new ArrayList<>(parent.children())) {
                if (child instanceof MdAstNode childNode) {
                    visit(childNode, source, definitions);
                }
            }
        }
    }

    private static void enhanceTable(GfmTable table, String source, Map<String, String> definitions) {
        List<GfmTableRow> rows = table.children();
        if (rows.size() > MAX_TABLE_ROWS) {
            return;
        }
        for (GfmTableRow row : rows) {
            if (row.children()
                .size() > MAX_TABLE_CELLS_PER_ROW) {
                return;
            }
        }
        String[] sourceLines = source == null ? new String[0] : source.split("\\n", -1);
        normalizeMultilineRows(rows, sourceLines);
        if (!rows.isEmpty() && isSyntheticHeader(rows.getFirst())) {
            table.headerless = true;
            rows.removeFirst();
        }
        List<GfmTableCell> activeCells = new ArrayList<>();
        List<GfmTableCell> previousRowCells = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            GfmTableRow row = rows.get(rowIndex);
            List<GfmTableCell> sourceCells = new ArrayList<>(row.children());
            List<RawCell> rawCells = rawCellsForRow(sourceLines, row);
            List<GfmTableCell> retained = new ArrayList<>();
            List<GfmTableCell> rowCoverage = new ArrayList<>(activeCells);
            int rawIndex = 0;
            int cursor = 0;

            for (int sourceIndex = 0; sourceIndex < sourceCells.size(); sourceIndex++) {
                GfmTableCell cell = sourceCells.get(sourceIndex);
                RawCell rawCell = rawIndex < rawCells.size() ? rawCells.get(rawIndex) : null;

                if (rawCell != null && rawCell.continuation()
                    && cell.toText()
                        .trim()
                        .isEmpty()) {
                    rawIndex++;
                    continue;
                }

                applyCellAttributes(cell, definitions);
                boolean continueAbove = stripPrefix(cell, "^^");
                applyEdgeAlignment(cell);
                int columnSpan = 1;
                rawIndex++;
                while (rawIndex < rawCells.size() && rawCells.get(rawIndex)
                    .continuation()) {
                    columnSpan++;
                    rawIndex++;
                    if (sourceIndex + 1 < sourceCells.size() && sourceCells.get(sourceIndex + 1)
                        .toText()
                        .trim()
                        .isEmpty()) {
                        sourceIndex++;
                    }
                }

                if (continueAbove) {
                    GfmTableCell target = findCellCoveringColumn(previousRowCells, cursor);
                    if (target != null) {
                        appendCellContent(target, cell);
                        target.rowSpan++;
                        target.attributes.putAll(cell.attributes);
                        if (!rowCoverage.contains(target)) {
                            rowCoverage.add(target);
                        }
                        cursor = targetColumnEnd(target);
                        continue;
                    }
                }

                while (isOccupied(rowCoverage, cursor)) {
                    cursor++;
                }
                cell.columnSpan = Math.max(1, columnSpan);
                cell.rowSpan = Math.max(1, cell.rowSpan);
                cell.resolvedRowIndex = rowIndex;
                cell.resolvedColumnIndex = cursor;
                retained.add(cell);
                cursor += cell.columnSpan;
            }

            row.children()
                .clear();
            row.children()
                .addAll(retained);
            previousRowCells = new ArrayList<>(rowCoverage);
            previousRowCells.addAll(retained);
            activeCells = new ArrayList<>();
            for (GfmTableCell cell : rowCoverage) {
                if (rowIndex + 1 < cell.resolvedRowIndex + cell.rowSpan) {
                    activeCells.add(cell);
                }
            }
        }
    }

    private static boolean isSyntheticHeader(GfmTableRow row) {
        return row.children()
            .stream()
            .anyMatch(
                cell -> cell.toText()
                    .contains(MarkdownTableSourceNormalizer.HEADERLESS_MARKER));
    }

    private static void normalizeMultilineRows(List<GfmTableRow> rows, String[] lines) {
        if (lines.length == 0) {
            return;
        }
        for (int index = 0; index + 1 < rows.size();) {
            GfmTableRow row = rows.get(index);
            int line = row.position() != null && row.position()
                .start() != null ? row.position()
                    .start()
                    .line() - 1 : -1;
            if (line < 0 || line >= lines.length || !hasContinuationMarker(lines[line])) {
                index++;
                continue;
            }
            GfmTableRow continuation = rows.get(index + 1);
            List<GfmTableCell> targets = row.children();
            List<GfmTableCell> additions = continuation.children();
            int count = Math.max(targets.size(), additions.size());
            for (int column = 0; column < count; column++) {
                if (column >= targets.size()) {
                    targets.add(additions.get(column));
                    continue;
                }
                if (column >= additions.size()) {
                    continue;
                }
                GfmTableCell target = targets.get(column);
                GfmTableCell addition = additions.get(column);
                stripLineContinuation(target);
                appendCellContent(target, addition);
            }
            rows.remove(index + 1);
        }
        for (GfmTableRow row : rows) {
            for (GfmTableCell cell : row.children()) {
                stripLineContinuation(cell);
            }
        }
    }

    private static boolean hasContinuationMarker(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        int slashes = 0;
        while (end > 0 && line.charAt(end - 1) == '\\') {
            slashes++;
            end--;
        }
        return (slashes & 1) == 1;
    }

    private static void stripLineContinuation(GfmTableCell cell) {
        List<?> children = cell.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Object rawChild = children.get(index);
            if (!(rawChild instanceof MdAstText text)) {
                continue;
            }
            int end = text.value.length();
            while (end > 0 && Character.isWhitespace(text.value.charAt(end - 1))) {
                end--;
            }
            if (end > 0 && text.value.charAt(end - 1) == '\\') {
                text.value = text.value.substring(0, end - 1) + text.value.substring(end);
            }
            return;
        }
    }

    private static void applyCellAttributes(GfmTableCell cell, Map<String, String> definitions) {
        String text = cell.toText();
        Matcher matcher = INLINE_ATTRIBUTES.matcher(text);
        if (!matcher.find()) {
            return;
        }
        cell.attributes.putAll(parseAttributes(matcher.group(1), definitions, 0));
        stripSuffix(cell, matcher.start());
    }

    private static Map<String, String> collectDefinitions(String source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> definitions = new LinkedHashMap<>();
        for (String line : source.split("\\n", -1)) {
            Matcher matcher = ATTRIBUTE_DEFINITION.matcher(line);
            if (matcher.matches()) {
                String name = matcher.group(1);
                String value = matcher.group(2);
                definitions.compute(name, (k, existing) -> existing == null ? value : existing + " " + value);
            }
        }
        return definitions;
    }

    private static Map<String, String> parseAttributes(String source, Map<String, String> definitions, int depth) {
        if (source == null || source.isBlank() || depth > 8) {
            return Collections.emptyMap();
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        for (String token : tokenize(source)) {
            if (token.startsWith("#") && token.length() > 1) {
                attributes.put("id", token.substring(1));
            } else if (token.startsWith(".") && token.length() > 1) {
                attributes.compute(
                    "class",
                    (k, classes) -> classes == null || classes.isEmpty() ? token.substring(1)
                        : classes + " " + token.substring(1));
            } else {
                int equals = token.indexOf('=');
                if (equals > 0) {
                    String key = token.substring(0, equals)
                        .toLowerCase();
                    if (isSafeAttribute(key)) {
                        attributes.put(key, unquote(token.substring(equals + 1)));
                    }
                } else if (definitions.containsKey(token)) {
                    attributes.putAll(parseAttributes(definitions.get(token), definitions, depth + 1));
                }
            }
        }
        return attributes;
    }

    private static boolean isSafeAttribute(String key) {
        return key.equals("id") || key.equals("class")
            || key.equals("style")
            || key.equals("align")
            || key.equals("valign")
            || key.equals("color")
            || key.equals("bgcolor")
            || key.equals("background")
            || key.equals("widths");
    }

    private static List<String> tokenize(String source) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quote != 0) {
                token.append(character);
                if (character == quote) {
                    quote = 0;
                }
            } else if (character == '\'' || character == '"') {
                quote = character;
                token.append(character);
            } else if (Character.isWhitespace(character)) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(character);
            }
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
            && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void applyEdgeAlignment(GfmTableCell cell) {
        String text = cell.toText()
            .trim();
        boolean left = text.startsWith(":");
        boolean right = text.endsWith(":");
        if (!left && !right) {
            return;
        }
        cell.attributes.put("align", left && right ? "center" : left ? "left" : "right");
        if (left) {
            stripPrefix(cell, ":");
        }
        if (right) {
            stripTrailingCharacter(cell, ':');
        }
    }

    private static boolean stripPrefix(GfmTableCell cell, String prefix) {
        for (var child : cell.children()) {
            if (child instanceof MdAstText text) {
                String value = text.value;
                int start = 0;
                while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
                    start++;
                }
                if (value.regionMatches(start, prefix, 0, prefix.length())) {
                    text.value = value.substring(0, start) + value.substring(start + prefix.length());
                    return true;
                }
            }
        }
        return false;
    }

    private static void stripSuffix(GfmTableCell cell, int ignoredStart) {
        List<?> children = cell.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Object rawChild = children.get(index);
            if (!(rawChild instanceof MdAstNode child)) {
                continue;
            }
            if (child instanceof MdAstText text) {
                Matcher matcher = INLINE_ATTRIBUTES.matcher(text.value);
                if (matcher.find()) {
                    text.value = text.value.substring(0, matcher.start());
                }
                return;
            }
        }
    }

    private static void stripTrailingCharacter(GfmTableCell cell, char character) {
        List<?> children = cell.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Object rawChild = children.get(index);
            if (!(rawChild instanceof MdAstText text)) {
                continue;
            }
            int end = text.value.length();
            while (end > 0 && Character.isWhitespace(text.value.charAt(end - 1))) {
                end--;
            }
            if (end > 0 && text.value.charAt(end - 1) == character) {
                text.value = text.value.substring(0, end - 1) + text.value.substring(end);
            }
            return;
        }
    }

    private static void appendCellContent(GfmTableCell target, GfmTableCell continuation) {
        if (continuation.children()
            .isEmpty()) {
            return;
        }
        if (!target.children()
            .isEmpty()) {
            target.children()
                .add(new MdAstBreak());
        }
        target.children()
            .addAll(continuation.children());
    }

    private static GfmTableCell findCellCoveringColumn(List<GfmTableCell> cells, int column) {
        GfmTableCell result = null;
        for (GfmTableCell cell : cells) {
            if (column < cell.resolvedColumnIndex || column >= cell.resolvedColumnIndex + cell.columnSpan) {
                continue;
            }
            if (result == null || cell.resolvedRowIndex > result.resolvedRowIndex) {
                result = cell;
            }
        }
        return result;
    }

    private static int targetColumnEnd(GfmTableCell cell) {
        return cell.resolvedColumnIndex + cell.columnSpan;
    }

    private static boolean isOccupied(List<GfmTableCell> cells, int column) {
        for (GfmTableCell cell : cells) {
            if (column >= cell.resolvedColumnIndex && column < cell.resolvedColumnIndex + cell.columnSpan) {
                return true;
            }
        }
        return false;
    }

    private static List<RawCell> rawCellsForRow(String[] lines, GfmTableRow row) {
        if (lines.length == 0 || row.position() == null
            || row.position()
                .start() == null) {
            return List.of();
        }
        int line = row.position()
            .start()
            .line() - 1;
        return line >= 0 && line < lines.length ? splitRawRow(lines[line]) : List.of();
    }

    private static List<RawCell> splitRawRow(String line) {
        String text = line.trim();
        if (text.startsWith("|")) {
            text = text.substring(1);
        }
        List<RawCell> cells = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < text.length();) {
            char character = text.charAt(index);
            if (character == '\\' && index + 1 < text.length()) {
                value.append(character)
                    .append(text.charAt(index + 1));
                index += 2;
                continue;
            }
            if (character != '|') {
                value.append(character);
                index++;
                continue;
            }
            int end = index + 1;
            while (end < text.length() && text.charAt(end) == '|') {
                end++;
            }
            int run = end - index;
            cells.add(
                new RawCell(
                    value.toString()
                        .trim(),
                    false));
            value.setLength(0);
            for (int count = 1; count < run; count++) {
                cells.add(new RawCell("", true));
            }
            index = end;
        }
        if (!value.isEmpty()) {
            cells.add(
                new RawCell(
                    value.toString()
                        .trim(),
                    false));
        }
        return cells;
    }

    private static class RawCell {

        private final String value;
        private final boolean continuation;

        private RawCell(String value, boolean continuation) {
            this.value = value;
            this.continuation = continuation;
        }

        private boolean continuation() {
            return continuation;
        }
    }
}
