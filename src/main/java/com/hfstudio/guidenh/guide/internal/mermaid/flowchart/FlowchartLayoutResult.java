package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class FlowchartLayoutResult {

    public record NodeMinSize(int width, int height) {

        public NodeMinSize {
            width = Math.max(0, width);
            height = Math.max(0, height);
        }
    }

    private final Map<String, NodePosition> nodePositions;
    private final List<EdgePath> edgePaths;
    private final int width;
    private final int height;

    public FlowchartLayoutResult(Map<String, NodePosition> nodePositions, List<EdgePath> edgePaths, int width,
        int height) {
        this.nodePositions = nodePositions != null ? Collections.unmodifiableMap(new LinkedHashMap<>(nodePositions))
            : Map.of();
        this.edgePaths = edgePaths != null ? List.copyOf(new ArrayList<>(edgePaths)) : List.of();
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Nullable
    public NodePosition getPosition(String nodeId) {
        return nodePositions.get(nodeId);
    }

    @Getter
    public static class NodePosition {

        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public NodePosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        public int getCenterX() {
            return x + width / 2;
        }

        public int getCenterY() {
            return y + height / 2;
        }
    }

    public static class EdgePath {

        @Getter
        private final String fromId;
        @Getter
        private final String toId;
        @Getter
        private final List<Point> points;
        private final @Nullable String edgeId;

        public EdgePath(String fromId, String toId, List<Point> points) {
            this(fromId, toId, points, null);
        }

        public EdgePath(String fromId, String toId, List<Point> points, @Nullable String edgeId) {
            this.fromId = fromId != null ? fromId : "";
            this.toId = toId != null ? toId : "";
            this.points = points != null ? List.copyOf(new ArrayList<>(points)) : List.of();
            this.edgeId = edgeId;
        }

        public @Nullable String getEdgeId() {
            return edgeId;
        }
    }

    @Getter
    public static class Point {

        private final int x;
        private final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }
}
