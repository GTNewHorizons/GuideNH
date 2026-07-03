package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowchartDocument {

    private final FlowchartDirection direction;
    private final Map<String, FlowchartNode> nodes;
    private final List<FlowchartEdge> edges;
    private final List<FlowchartSubgraph> subgraphs;
    private final List<String> nodeOrder;

    public FlowchartDocument(FlowchartDirection direction, Map<String, FlowchartNode> nodes, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> subgraphs) {
        this.direction = direction != null ? direction : FlowchartDirection.TB;
        Map<String, FlowchartNode> src = nodes != null ? nodes : Map.of();
        this.nodes = Map.copyOf(new LinkedHashMap<>(src));
        this.edges = List.copyOf(new ArrayList<>(edges != null ? edges : List.of()));
        this.subgraphs = List.copyOf(new ArrayList<>(subgraphs != null ? subgraphs : List.of()));
        List<String> order = new ArrayList<>(src.keySet());
        this.nodeOrder = Collections.unmodifiableList(order);
    }

    public FlowchartDirection getDirection() {
        return direction;
    }

    public Map<String, FlowchartNode> getNodes() {
        return nodes;
    }

    public List<FlowchartEdge> getEdges() {
        return edges;
    }

    public List<FlowchartSubgraph> getSubgraphs() {
        return subgraphs;
    }

    public List<String> getNodeOrder() {
        return nodeOrder;
    }
}
