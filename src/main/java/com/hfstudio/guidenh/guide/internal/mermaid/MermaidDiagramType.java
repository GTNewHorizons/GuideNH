package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.List;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public enum MermaidDiagramType {

    MINDMAP,
    FLOWCHART,
    UNKNOWN;

    private static boolean isFrontmatterDelimiter(String line) {
        return "---".equals(line.trim());
    }

    public static MermaidDiagramType detect(String source) {
        if (source == null || source.isEmpty()) {
            return UNKNOWN;
        }
        String normalized = GuideStringLines.normalizeLineEndings(source);
        List<String> lines = GuideStringLines.splitLines(normalized);

        int i = 0;

        if (!lines.isEmpty() && isFrontmatterDelimiter(lines.getFirst())) {
            i++;
            while (i < lines.size() && !isFrontmatterDelimiter(lines.get(i))) {
                i++;
            }
            i++;
        }

        for (; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) {
                continue;
            }
            if (trimmed.startsWith("mindmap")) {
                return MINDMAP;
            }
            if (trimmed.startsWith("flowchart") || trimmed.startsWith("graph")) {
                return FLOWCHART;
            }
            break;
        }

        return UNKNOWN;
    }
}
