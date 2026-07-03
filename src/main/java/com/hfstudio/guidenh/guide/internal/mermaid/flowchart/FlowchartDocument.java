package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

public class FlowchartDocument {

    private final String sourceText;

    public FlowchartDocument(String sourceText) {
        this.sourceText = sourceText != null ? sourceText : "";
    }

    public String getSourceText() {
        return sourceText;
    }
}
