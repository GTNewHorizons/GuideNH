package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class FlowchartParser {

    protected FlowchartParser() {}

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(source);
    }

    public static FlowchartDocument parse(String source) {
        String normalized = normalize(source);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Flowchart source is empty");
        }
        return new FlowchartDocument(normalized);
    }
}
