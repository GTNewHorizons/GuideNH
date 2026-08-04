package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class FlowchartDocument {

    private final FlowchartDirection direction;
    private final Map<String, FlowchartNode> nodes;
    private final List<FlowchartEdge> edges;
    private final List<FlowchartSubgraph> subgraphs;
    private final List<String> nodeOrder;
    private final FlowchartLayoutMode layoutMode;
    private final FlowchartConfig config;
    @Nullable
    private final String copyValue;

    public record FlowchartConfig(int nodeSpacing, int rankSpacing, int canvasPadding) {

        public static final FlowchartConfig DEFAULT = new FlowchartConfig(20, 20, 20);
    }

    public FlowchartDocument(FlowchartDirection direction, Map<String, FlowchartNode> nodes, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> subgraphs) {
        this(direction, nodes, edges, subgraphs, null, null, null);
    }

    public FlowchartDocument(FlowchartDirection direction, Map<String, FlowchartNode> nodes, List<FlowchartEdge> edges,
        List<FlowchartSubgraph> subgraphs, @Nullable FlowchartLayoutMode layoutMode, @Nullable FlowchartConfig config,
        @Nullable String copyValue) {
        this.direction = direction != null ? direction : FlowchartDirection.TB;
        Map<String, FlowchartNode> src = nodes != null ? nodes : Map.of();
        // NB: NOT Map.copyOf. JDK 9+ ImmutableCollections.MapN salts its hash
        // table with a per-JVM random value (SALT32L = const * System.nanoTime()
        // >> 16), so Map.copyOf's iteration order varies across JVM runs for
        // the same content. Layout code iterates this map to order ELK node
        // creation (and node min-size computation), and ELK preserves that
        // order for equal-solution ties (isolated nodes, symmetric layers) —
        // a per-run order flip moved mermaid nodes between renders. An
        // unmodifiable LinkedHashMap keeps the deterministic parse order.
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(src));
        this.edges = List.copyOf(new ArrayList<>(edges != null ? edges : List.of()));
        this.subgraphs = List.copyOf(new ArrayList<>(subgraphs != null ? subgraphs : List.of()));
        this.layoutMode = layoutMode != null ? layoutMode : FlowchartLayoutMode.BUILTIN;
        this.config = config != null ? config : FlowchartConfig.DEFAULT;
        this.copyValue = copyValue;
        List<String> order = new ArrayList<>(src.keySet());
        this.nodeOrder = Collections.unmodifiableList(order);
    }
}
