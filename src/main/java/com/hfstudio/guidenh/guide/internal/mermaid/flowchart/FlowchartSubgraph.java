package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class FlowchartSubgraph {

    @Getter
    private final String id;
    @Getter
    private final String label;
    @Getter
    private final List<String> nodeIds;
    @Getter
    private final List<FlowchartEdge> edges;
    @Getter
    private final List<FlowchartSubgraph> children;
    @Nullable
    private final FlowchartDirection direction;

    public FlowchartSubgraph(String id, @Nullable String label, List<String> nodeIds, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> children) {
        this(id, label, nodeIds, edges, children, null);
    }

    public FlowchartSubgraph(String id, @Nullable String label, List<String> nodeIds, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> children, @Nullable FlowchartDirection direction) {
        this.id = id != null ? id : "";
        this.label = label != null ? label : this.id;
        this.nodeIds = List.copyOf(new ArrayList<>(nodeIds != null ? nodeIds : List.of()));
        this.edges = List.copyOf(new ArrayList<>(edges != null ? edges : List.of()));
        this.children = List.copyOf(new ArrayList<>(children != null ? children : List.of()));
        this.direction = direction;
    }

    public @Nullable FlowchartDirection getDirection() {
        return direction;
    }
}
