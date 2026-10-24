package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MarkdownTableSourceNormalizer {

    public static final String HEADERLESS_MARKER = "\uE000guidenh-headerless";

    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})");
    private static final Pattern DELIMITER = Pattern
        .compile("^\\s*\\|?\\s*:?-{1,}:?\\s*(?:\\|\\s*:?-{1,}:?\\s*)+\\|?\\s*$");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\s*\\|?.*\\|.*$");

    private MarkdownTableSourceNormalizer() {}

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return source == null ? "" : source;
        }
        String[] lines = source.replace("\r\n", "\n")
            .replace('\r', '\n')
            .split("\n", -1);
        List<String> normalized = new ArrayList<>(lines.length + 8);
        String fenceMarker = null;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            var fence = FENCE.matcher(line);
            if (fence.find()) {
                String marker = fence.group(1);
                if (fenceMarker == null) {
                    fenceMarker = marker;
                } else if (marker.charAt(0) == fenceMarker.charAt(0) && marker.length() >= fenceMarker.length()) {
                    fenceMarker = null;
                }
                normalized.add(line);
                continue;
            }
            if (fenceMarker == null && isHeaderlessDelimiter(lines, index)) {
                int columnCount = countColumns(line);
                StringBuilder header = new StringBuilder("|");
                StringBuilder delimiter = new StringBuilder("|");
                for (int column = 0; column < columnCount; column++) {
                    header.append(' ')
                        .append(HEADERLESS_MARKER)
                        .append(" |");
                    delimiter.append(" --- |");
                }
                normalized.add(header.toString());
                normalized.add(delimiter.toString());
                continue;
            }
            normalized.add(line);
        }
        return String.join("\n", normalized);
    }

    private static boolean isHeaderlessDelimiter(String[] lines, int index) {
        if (!DELIMITER.matcher(lines[index])
            .matches() || index + 1 >= lines.length
            || !TABLE_ROW.matcher(lines[index + 1])
                .matches()
            || lines[index + 1].isBlank()) {
            return false;
        }
        if (index > 0 && TABLE_ROW.matcher(lines[index - 1])
            .matches()
            && !DELIMITER.matcher(lines[index - 1])
                .matches()) {
            return false;
        }
        return true;
    }

    private static int countColumns(String line) {
        String text = line.trim();
        if (text.startsWith("|")) {
            text = text.substring(1);
        }
        int columns = 1;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '|') {
                columns++;
            }
        }
        if (text.endsWith("|")) {
            columns--;
        }
        return Math.max(1, columns);
    }
}
