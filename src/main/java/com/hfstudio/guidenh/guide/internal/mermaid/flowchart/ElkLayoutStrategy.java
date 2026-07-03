package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ElkLayoutStrategy implements FlowchartLayoutStrategy {

    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int GAP = 20;
    private static final int PADDING = 20;

    @Override
    public String getName() {
        return "elk";
    }

    @Override
    public FlowchartLayoutResult layout(FlowchartDocument document) {
        Map<String, FlowchartNode> nodes = document.getNodes();
        if (nodes.isEmpty()) {
            return new FlowchartLayoutResult(Map.of(), List.of(), 0, 0);
        }

        FlowchartDirection direction = document.getDirection();
        List<String> nodeOrder = document.getNodeOrder();
        Map<String, FlowchartLayoutResult.NodePosition> positions = new LinkedHashMap<>();
        List<FlowchartLayoutResult.EdgePath> edgePaths = new ArrayList<>();

        int maxW = 0;
        int maxH = 0;

        for (int i = 0; i < nodeOrder.size(); i++) {
            String id = nodeOrder.get(i);
            if (!nodes.containsKey(id)) continue;

            int x, y;
            switch (direction) {
                case LR:
                    x = PADDING + i * (NODE_WIDTH + GAP);
                    y = PADDING;
                    break;
                case RL:
                    x = PADDING + (nodeOrder.size() - 1 - i) * (NODE_WIDTH + GAP);
                    y = PADDING;
                    break;
                case BT:
                    x = PADDING;
                    y = PADDING + (nodeOrder.size() - 1 - i) * (NODE_HEIGHT + GAP);
                    break;
                default: // TB
                    x = PADDING;
                    y = PADDING + i * (NODE_HEIGHT + GAP);
                    break;
            }

            positions.put(id, new FlowchartLayoutResult.NodePosition(x, y, NODE_WIDTH, NODE_HEIGHT));
            maxW = Math.max(maxW, x + NODE_WIDTH);
            maxH = Math.max(maxH, y + NODE_HEIGHT);
        }

        for (FlowchartEdge edge : document.getEdges()) {
            FlowchartLayoutResult.NodePosition from = positions.get(edge.getFrom());
            FlowchartLayoutResult.NodePosition to = positions.get(edge.getTo());
            if (from == null || to == null) continue;

            List<FlowchartLayoutResult.Point> points = new ArrayList<>();
            points.add(new FlowchartLayoutResult.Point(from.getCenterX(), from.getCenterY()));
            points.add(new FlowchartLayoutResult.Point(to.getCenterX(), to.getCenterY()));
            edgePaths.add(new FlowchartLayoutResult.EdgePath(edge.getFrom(), edge.getTo(), points));
        }

        return new FlowchartLayoutResult(positions, edgePaths, maxW + PADDING, maxH + PADDING);
    }
}
