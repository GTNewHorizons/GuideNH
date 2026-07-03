package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;

public class FlowchartEdge {
    private final String from;
    private final String to;
    @Nullable private final String label;
    private final MermaidEdgeStyle style;
    @Nullable private final String edgeId;
    private final int length;

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style) {
        this(from, to, label, style, null, 0);
    }

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style,
        @Nullable String edgeId, int length) {
        this.from = Objects.requireNonNullElse(from, "");
        this.to = Objects.requireNonNullElse(to, "");
        this.label = label;
        this.style = style != null ? style : MermaidEdgeStyle.SOLID;
        this.edgeId = edgeId;
        this.length = Math.max(0, length);
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public @Nullable String getLabel() { return label; }
    public MermaidEdgeStyle getStyle() { return style; }
    public @Nullable String getEdgeId() { return edgeId; }
    public int getLength() { return length; }
}
