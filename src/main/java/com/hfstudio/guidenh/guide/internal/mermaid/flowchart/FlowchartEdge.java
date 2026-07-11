package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;

import lombok.Getter;

public class FlowchartEdge {

    @Getter
    private final String from;
    @Getter
    private final String to;
    @Nullable
    private final String label;
    @Getter
    private final MermaidEdgeStyle style;
    @Getter
    private final boolean arrowFwd;
    @Getter
    private final boolean arrowRev;
    @Getter
    private final MermaidArrowHead forwardHead;
    @Getter
    private final MermaidArrowHead reverseHead;
    @Nullable
    private final String edgeId;
    @Getter
    private final int length;
    @Nullable
    private final String styleOverride;

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style, boolean arrowFwd,
        boolean arrowRev, MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, @Nullable String edgeId,
        int length) {
        this(from, to, label, style, arrowFwd, arrowRev, forwardHead, reverseHead, edgeId, length, null);
    }

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style, boolean arrowFwd,
        boolean arrowRev, MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, @Nullable String edgeId,
        int length, @Nullable String styleOverride) {
        this.from = Objects.requireNonNullElse(from, "");
        this.to = Objects.requireNonNullElse(to, "");
        this.label = label;
        this.style = style != null ? style : MermaidEdgeStyle.SOLID;
        this.arrowFwd = arrowFwd;
        this.arrowRev = arrowRev;
        this.forwardHead = forwardHead != null ? forwardHead : MermaidArrowHead.NONE;
        this.reverseHead = reverseHead != null ? reverseHead : MermaidArrowHead.NONE;
        this.edgeId = edgeId;
        this.length = Math.max(0, length);
        this.styleOverride = styleOverride;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public @Nullable String getEdgeId() {
        return edgeId;
    }

    public @Nullable String getStyleOverride() {
        return styleOverride;
    }
}
