package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.options.SizeConstraint;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.jetbrains.annotations.Nullable;

public class ElkLayoutStrategy implements FlowchartLayoutStrategy {

    private static final int NODE_WIDTH = 80;
    private static final int NODE_HEIGHT = 30;
    private static final int SUBGRAPH_PADDING = 8;

    private static volatile boolean warmedUp;

    public static void warmup() {
        if (warmedUp) return;
        try {
            ElkNode root = ElkGraphUtil.createGraph();
            root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
            root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
            ElkNode a = ElkGraphUtil.createNode(root);
            a.setWidth(80);
            a.setHeight(30);
            ElkNode b = ElkGraphUtil.createNode(root);
            b.setWidth(80);
            b.setHeight(30);
            ElkGraphUtil.createSimpleEdge(a, b);
            RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
            engine.layout(root, new NullElkProgressMonitor());

            warmedUp = true;
        } catch (Exception ignored) {}
    }

    @Override
    public String getName() {
        return "elk";
    }

    // Subgraphs become ElkNode compounds with
    // SEPARATE_CHILDREN (so each gets its own layout run with its own DIRECTION).
    // Cross-hierarchy edges are split at the compound boundary: internal segments
    // (source→compound port) and external segments (compound port→target in parent).
    // ElkEdge references are stored so we can extract and stitch the sections
    // after layout without fragile key matching.

    @Override
    public FlowchartLayoutResult layout(FlowchartDocument document,
        Map<String, FlowchartLayoutResult.NodeMinSize> nodeMinSizes,
        Map<FlowchartEdge, FlowchartLayoutResult.LabelSize> edgeLabelSizes,
        Map<String, FlowchartLayoutResult.LabelSize> subgraphLabelSizes) {
        Map<String, FlowchartNode> nodes = document.getNodes();
        if (nodes.isEmpty()) {
            return new FlowchartLayoutResult(Map.of(), Map.of(), Map.of(), Map.of(), List.of(), 0, 0);
        }

        Map<String, FlowchartSubgraph> nodeToSubgraph = buildNodeToSubgraphMap(document.getSubgraphs());
        Map<String, FlowchartSubgraph> subgraphById = buildSubgraphByIdMap(document.getSubgraphs());
        Map<String, String> parentSgMap = buildParentSubgraphMap(document.getSubgraphs());
        var cfg = document.getConfig();

        Set<String> compoundSgIds = subgraphById.keySet();

        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, toElkDirection(document.getDirection()));
        root.setProperty(CoreOptions.SPACING_NODE_NODE, (double) cfg.nodeSpacing());
        root.setProperty(CoreOptions.SPACING_EDGE_NODE, (double) cfg.nodeSpacing());
        root.setProperty(CoreOptions.PADDING, new ElkPadding(cfg.canvasPadding()));

        // Create compound nodes
        Map<String, ElkNode> compoundMap = new LinkedHashMap<>();
        for (FlowchartSubgraph sg : document.getSubgraphs()) {
            createCompounds(sg, root, compoundMap, document.getDirection(), cfg, subgraphLabelSizes);
        }

        // Create leaf nodes inside their parents
        Map<String, ElkNode> elkNodeMap = new LinkedHashMap<>();
        for (FlowchartNode node : nodes.values()) {
            ElkNode parent = nodeParent(
                node.getId(),
                nodeToSubgraph,
                compoundSgIds,
                parentSgMap,
                root,
                compoundMap,
                subgraphById);
            ElkNode elkNode = ElkGraphUtil.createNode(parent);
            elkNode.setIdentifier(node.getId());
            FlowchartLayoutResult.NodeMinSize minSize = nodeMinSizes != null ? nodeMinSizes.get(node.getId()) : null;
            elkNode.setWidth(minSize != null ? Math.max(NODE_WIDTH, minSize.width()) : NODE_WIDTH);
            elkNode.setHeight(minSize != null ? Math.max(NODE_HEIGHT, minSize.height()) : NODE_HEIGHT);
            elkNodeMap.put(node.getId(), elkNode);
        }

        // Create edges — for cross-hierarchy edges we build a chain of ports
        // through every compound ancestor so ELK can route each segment within
        // a single layout run. The chain is later stitched back into one path.
        List<SplitChain> splitChains = new ArrayList<>();
        Set<String> dummyNodeIds = new LinkedHashSet<>();
        List<DummyChain> dummyChains = new ArrayList<>();
        Map<ElkEdge, String> simpleEdgeToId = new LinkedHashMap<>();
        Map<FlowchartEdge, ElkLabel> edgeLabels = new LinkedHashMap<>();
        int[] dummyCounter = { 0 };

        for (FlowchartEdge edge : document.getEdges()) {
            ElkNode source = elkNodeMap.get(edge.getFrom());
            ElkNode target = elkNodeMap.get(edge.getTo());
            if (source == null || target == null) continue;

            List<String> srcChain = compoundChain(
                edge.getFrom(),
                nodeToSubgraph,
                compoundSgIds,
                parentSgMap,
                subgraphById);
            List<String> tgtChain = compoundChain(
                edge.getTo(),
                nodeToSubgraph,
                compoundSgIds,
                parentSgMap,
                subgraphById);

            // Find the deepest common container
            int commonLen = 0;
            while (commonLen < srcChain.size() && commonLen < tgtChain.size()
                && srcChain.get(commonLen)
                    .equals(tgtChain.get(commonLen))) {
                commonLen++;
            }

            if (commonLen == srcChain.size() && commonLen == tgtChain.size()) {
                // Both nodes share the same deepest compound → simple edge
                int edgeLength = edge.getLength();
                int dummyCount = Math.max(0, edgeLength - 3);
                if (dummyCount > 0) {
                    ElkNode parent = source.getParent();
                    List<ElkNode> dummies = new ArrayList<>();
                    List<ElkEdge> chainEdges = new ArrayList<>();
                    for (int i = 0; i < dummyCount; i++) {
                        ElkNode dummy = ElkGraphUtil.createNode(parent);
                        String dummyId = "__dummy_" + (dummyCounter[0]++);
                        dummy.setIdentifier(dummyId);
                        dummy.setWidth(1);
                        dummy.setHeight(1);
                        dummyNodeIds.add(dummyId);
                        dummies.add(dummy);
                    }
                    ElkConnectableShape prev = source;
                    for (ElkNode dummy : dummies) {
                        chainEdges.add(ElkGraphUtil.createSimpleEdge(prev, dummy));
                        prev = dummy;
                    }
                    chainEdges.add(ElkGraphUtil.createSimpleEdge(prev, target));
                    addEdgeLabel(edge, chainEdges.get(chainEdges.size() / 2), edgeLabelSizes, edgeLabels);
                    dummyChains.add(new DummyChain(edge.getFrom(), edge.getTo(), edge.getEdgeId(), chainEdges));
                } else {
                    ElkEdge elkEdge = ElkGraphUtil.createSimpleEdge(source, target);
                    addEdgeLabel(edge, elkEdge, edgeLabelSizes, edgeLabels);
                    String eid = edge.getEdgeId();
                    if (eid != null) {
                        simpleEdgeToId.put(elkEdge, eid);
                    }
                }
                continue;
            }

            // Build port chain: create a port on each non-common compound.
            // Each internal edge is stored so the stitching loop can find it
            // without relying on post-layout edge lookups.
            List<ElkPort> srcPorts = new ArrayList<>();
            List<ElkPort> tgtPorts = new ArrayList<>();
            List<ElkEdge> srcEdges = new ArrayList<>();
            List<ElkEdge> tgtEdges = new ArrayList<>();

            ElkConnectableShape prevSrc = source;
            for (int i = srcChain.size() - 1; i >= commonLen; i--) {
                ElkNode compound = compoundMap.get(srcChain.get(i));
                ElkPort port = ElkGraphUtil.createPort(compound);
                srcEdges.add(ElkGraphUtil.createSimpleEdge(prevSrc, port));
                srcPorts.add(port);
                prevSrc = port;
            }

            ElkConnectableShape prevTgt = target;
            for (int i = tgtChain.size() - 1; i >= commonLen; i--) {
                ElkNode compound = compoundMap.get(tgtChain.get(i));
                ElkPort port = ElkGraphUtil.createPort(compound);
                tgtEdges.add(ElkGraphUtil.createSimpleEdge(port, prevTgt));
                tgtPorts.add(port);
                prevTgt = port;
            }

            // External edge: connects the outermost source and target (ports or nodes)
            ElkConnectableShape extSrc = !srcPorts.isEmpty() ? srcPorts.get(srcPorts.size() - 1) : source;
            ElkConnectableShape extTgt = !tgtPorts.isEmpty() ? tgtPorts.get(tgtPorts.size() - 1) : target;
            ElkEdge externalEdge = ElkGraphUtil.createSimpleEdge(extSrc, extTgt);
            addEdgeLabel(edge, externalEdge, edgeLabelSizes, edgeLabels);

            splitChains.add(
                new SplitChain(
                    edge.getFrom(),
                    edge.getTo(),
                    edge.getEdgeId(),
                    srcPorts,
                    tgtPorts,
                    externalEdge,
                    srcEdges,
                    tgtEdges));
        }

        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        Map<String, FlowchartLayoutResult.NodePosition> positions = new LinkedHashMap<>();
        collectPositions(root, 0, 0, cfg.canvasPadding(), positions, elkNodeMap);
        List<FlowchartLayoutResult.EdgePath> edgePaths = new ArrayList<>();

        // Collect all edges created by split chains and dummy chains so we can skip them in collectRemainingEdges
        Set<ElkEdge> splitEdges = new LinkedHashSet<>();

        // Stitch split edges from stored port chains.
        // Each segment has its own ElkPort endpoints as logical anchors.
        // We extract internal bend points from each segment in local coords,
        // then use the ElkPort root position as the authoritative stitch point.
        for (SplitChain sc : splitChains) {

            List<ElkEdge> chainEdges = new ArrayList<>(sc.srcEdges);
            chainEdges.add(sc.externalEdge);
            chainEdges.addAll(sc.tgtEdges);
            splitEdges.addAll(chainEdges);

            // External edge (already root-relative)
            List<FlowchartLayoutResult.Point> extPts = edgePoints(sc.externalEdge, root, cfg.canvasPadding());
            if (extPts.isEmpty()) continue;
            List<FlowchartLayoutResult.Point> merged = new ArrayList<>(extPts);

            // Prepend source internal segments (outermost to innermost).
            for (int i = sc.srcPorts.size() - 1; i >= 0; i--) {
                ElkPort port = sc.srcPorts.get(i);
                ElkEdge internalEdge = sc.srcEdges.get(i);
                List<FlowchartLayoutResult.Point> intPts = edgePoints(internalEdge, 0.0, 0.0, cfg.canvasPadding());
                if (intPts.size() < 2) continue;
                double[] portRoot = nodeOffsetExact(port.getParent(), root);
                int anchorX = cfg.canvasPadding() + (int) Math.round(portRoot[0] + port.getX());
                int anchorY = cfg.canvasPadding() + (int) Math.round(portRoot[1] + port.getY());
                FlowchartLayoutResult.Point last = intPts.get(intPts.size() - 1);
                int dx = anchorX - last.getX();
                int dy = anchorY - last.getY();
                List<FlowchartLayoutResult.Point> prepend = new ArrayList<>();
                for (int j = 0; j < intPts.size() - 1; j++) {
                    prepend.add(
                        new FlowchartLayoutResult.Point(
                            intPts.get(j)
                                .getX() + dx,
                            intPts.get(j)
                                .getY() + dy));
                }
                merged.addAll(0, prepend);
            }

            // Append target internal segments (innermost to outermost).
            for (int i = sc.tgtPorts.size() - 1; i >= 0; i--) {
                ElkPort port = sc.tgtPorts.get(i);
                ElkEdge internalEdge = sc.tgtEdges.get(i);
                List<FlowchartLayoutResult.Point> intPts = edgePoints(internalEdge, 0.0, 0.0, cfg.canvasPadding());
                if (intPts.size() < 2) continue;
                double[] portRoot = nodeOffsetExact(port.getParent(), root);
                int anchorX = cfg.canvasPadding() + (int) Math.round(portRoot[0] + port.getX());
                int anchorY = cfg.canvasPadding() + (int) Math.round(portRoot[1] + port.getY());
                FlowchartLayoutResult.Point first = intPts.get(0);
                int dx = anchorX - first.getX();
                int dy = anchorY - first.getY();
                for (int j = 1; j < intPts.size(); j++) {
                    merged.add(
                        new FlowchartLayoutResult.Point(
                            intPts.get(j)
                                .getX() + dx,
                            intPts.get(j)
                                .getY() + dy));
                }
            }

            if (!merged.isEmpty()) {
                edgePaths.add(new FlowchartLayoutResult.EdgePath(sc.sourceId, sc.targetId, merged, sc.edgeId));
            }
        }

        // Stitch dummy chain edges into single paths.
        for (DummyChain dc : dummyChains) {
            splitEdges.addAll(dc.chainEdges);
            List<FlowchartLayoutResult.Point> merged = new ArrayList<>();
            for (ElkEdge subEdge : dc.chainEdges) {
                List<FlowchartLayoutResult.Point> pts = edgePoints(subEdge, root, cfg.canvasPadding());
                if (pts.isEmpty()) continue;
                if (merged.isEmpty()) {
                    merged.addAll(pts);
                } else {
                    merged.addAll(pts.subList(1, pts.size()));
                }
            }
            if (!merged.isEmpty()) {
                edgePaths.add(new FlowchartLayoutResult.EdgePath(dc.sourceId, dc.targetId, merged, dc.edgeId));
            }
        }

        // Collect unsplit edges from the whole tree (skip hierarchical and chain edges)
        collectRemainingEdges(root, 0, 0, cfg.canvasPadding(), edgePaths, splitEdges, simpleEdgeToId);

        Map<String, FlowchartLayoutResult.NodePosition> subgraphPositions = new LinkedHashMap<>();
        Map<String, FlowchartLayoutResult.NodePosition> subgraphLabelPositions = new LinkedHashMap<>();
        for (var entry : compoundMap.entrySet()) {
            ElkNode compound = entry.getValue();
            double[] offset = nodeOffsetExact(compound, root);
            subgraphPositions.put(
                entry.getKey(),
                new FlowchartLayoutResult.NodePosition(
                    cfg.canvasPadding() + (int) Math.round(offset[0]),
                    cfg.canvasPadding() + (int) Math.round(offset[1]),
                    (int) Math.ceil(compound.getWidth()),
                    (int) Math.ceil(compound.getHeight())));
            FlowchartLayoutResult.LabelSize label = subgraphLabelSizes.get(entry.getKey());
            if (label != null && label.height() > 0) {
                subgraphLabelPositions.put(
                    entry.getKey(),
                    new FlowchartLayoutResult.NodePosition(
                        cfg.canvasPadding() + (int) Math.round(offset[0] + (compound.getWidth() - label.width()) / 2),
                        cfg.canvasPadding() + (int) Math.round(offset[1]) + 2,
                        label.width(),
                        Math.max(1, label.height() - 4)));
            }
        }
        Map<FlowchartEdge, FlowchartLayoutResult.NodePosition> labelPositions = new LinkedHashMap<>();
        for (var entry : edgeLabels.entrySet()) {
            ElkLabel label = entry.getValue();
            ElkEdge edge = (ElkEdge) label.eContainer();
            double[] offset = nodeOffsetExact(edge.getContainingNode(), root);
            labelPositions.put(
                entry.getKey(),
                new FlowchartLayoutResult.NodePosition(
                    cfg.canvasPadding() + (int) Math.round(offset[0] + label.getX()),
                    cfg.canvasPadding() + (int) Math.round(offset[1] + label.getY()),
                    (int) Math.ceil(label.getWidth()),
                    (int) Math.ceil(label.getHeight())));
        }
        return new FlowchartLayoutResult(
            positions,
            subgraphPositions,
            subgraphLabelPositions,
            labelPositions,
            edgePaths,
            (int) Math.ceil(root.getWidth()) + cfg.canvasPadding() * 2,
            (int) Math.ceil(root.getHeight()) + cfg.canvasPadding() * 2);
    }

    private static void addEdgeLabel(FlowchartEdge edge, ElkEdge elkEdge,
        Map<FlowchartEdge, FlowchartLayoutResult.LabelSize> sizes, Map<FlowchartEdge, ElkLabel> labels) {
        FlowchartLayoutResult.LabelSize size = sizes.get(edge);
        if (size == null) return;
        ElkLabel label = ElkGraphUtil.createLabel(elkEdge);
        label.setText(edge.getLabel());
        label.setDimensions(size.width(), size.height());
        label.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.CENTER);
        labels.put(edge, label);
    }

    private static List<FlowchartLayoutResult.Point> edgePoints(ElkEdge edge, ElkNode root, int padding) {
        ElkNode container = edge.getContainingNode();
        double[] off = nodeOffsetExact(container, root);
        return edgePoints(edge, off[0], off[1], padding);
    }

    private static List<FlowchartLayoutResult.Point> edgePoints(ElkEdge edge, double offsetX, double offsetY,
        int padding) {
        List<FlowchartLayoutResult.Point> pts = new ArrayList<>();
        for (ElkEdgeSection section : edge.getSections()) {
            pts.add(
                new FlowchartLayoutResult.Point(
                    padding + (int) Math.round(offsetX + section.getStartX()),
                    padding + (int) Math.round(offsetY + section.getStartY())));
            for (var bp : section.getBendPoints()) {
                pts.add(
                    new FlowchartLayoutResult.Point(
                        padding + (int) Math.round(offsetX + bp.getX()),
                        padding + (int) Math.round(offsetY + bp.getY())));
            }
            pts.add(
                new FlowchartLayoutResult.Point(
                    padding + (int) Math.round(offsetX + section.getEndX()),
                    padding + (int) Math.round(offsetY + section.getEndY())));
        }
        return pts;
    }

    private static double[] nodeOffsetExact(ElkNode node, ElkNode root) {
        double ox = 0, oy = 0;
        ElkNode cur = node;
        while (cur != null && cur != root) {
            ox += cur.getX();
            oy += cur.getY();
            cur = cur.getParent();
        }
        return new double[] { ox, oy };
    }

    private static void collectRemainingEdges(ElkNode node, double offsetX, double offsetY, int padding,
        List<FlowchartLayoutResult.EdgePath> edgePaths, Set<ElkEdge> splitEdges, Map<ElkEdge, String> simpleEdgeToId) {
        double absX = offsetX + node.getX();
        double absY = offsetY + node.getY();

        for (ElkEdge edge : node.getContainedEdges()) {
            if (edge.isHierarchical()) continue;
            if (edge.getSources()
                .isEmpty()
                || edge.getTargets()
                    .isEmpty())
                continue;

            if (splitEdges.contains(edge)) continue;

            String srcId = ElkGraphUtil.connectableShapeToNode(
                edge.getSources()
                    .get(0))
                .getIdentifier();
            String tgtId = ElkGraphUtil.connectableShapeToNode(
                edge.getTargets()
                    .get(0))
                .getIdentifier();
            if (srcId == null || tgtId == null) continue;
            if (srcId.startsWith("__sg_") || tgtId.startsWith("__sg_")) continue;

            List<FlowchartLayoutResult.Point> pts = edgePoints(edge, absX, absY, padding);
            if (!pts.isEmpty()) {
                String eid = simpleEdgeToId.get(edge);
                edgePaths.add(new FlowchartLayoutResult.EdgePath(srcId, tgtId, pts, eid));
            }
        }

        for (ElkNode child : node.getChildren()) {
            collectRemainingEdges(child, absX, absY, padding, edgePaths, splitEdges, simpleEdgeToId);
        }
    }

    private static List<String> compoundChain(String nodeId, Map<String, FlowchartSubgraph> nodeToSubgraph,
        Set<String> compoundSgIds, Map<String, String> parentSgMap, Map<String, FlowchartSubgraph> subgraphById) {
        List<String> reverse = new ArrayList<>();
        FlowchartSubgraph sg = nodeToSubgraph.get(nodeId);
        while (sg != null) {
            if (compoundSgIds.contains(sg.getId())) {
                reverse.add(sg.getId());
            }
            String parentId = parentSgMap.get(sg.getId());
            sg = parentId != null ? subgraphById.get(parentId) : null;
        }
        List<String> chain = new ArrayList<>();
        for (int i = reverse.size() - 1; i >= 0; i--) {
            chain.add(reverse.get(i));
        }
        return chain;
    }

    private static Map<String, String> buildParentSubgraphMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, String> result = new LinkedHashMap<>();
        buildParentSubgraphMapRec(subgraphs, null, result);
        return result;
    }

    private static void buildParentSubgraphMapRec(List<FlowchartSubgraph> subgraphs, String parentId,
        Map<String, String> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            result.put(sg.getId(), parentId);
            buildParentSubgraphMapRec(sg.getChildren(), sg.getId(), result);
        }
    }

    private static Map<String, FlowchartSubgraph> buildSubgraphByIdMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, FlowchartSubgraph> result = new LinkedHashMap<>();
        buildSubgraphByIdMapRec(subgraphs, result);
        return result;
    }

    private static void buildSubgraphByIdMapRec(List<FlowchartSubgraph> subgraphs,
        Map<String, FlowchartSubgraph> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            result.put(sg.getId(), sg);
            buildSubgraphByIdMapRec(sg.getChildren(), result);
        }
    }

    private static void createCompounds(FlowchartSubgraph sg, ElkNode parent, Map<String, ElkNode> compoundMap,
        FlowchartDirection parentDirection, FlowchartDocument.FlowchartConfig cfg,
        Map<String, FlowchartLayoutResult.LabelSize> labelSizes) {
        FlowchartDirection direction = sg.getDirection() != null ? sg.getDirection() : parentDirection;
        ElkNode compound = ElkGraphUtil.createNode(parent);
        compound.setIdentifier("__sg_" + sg.getId());
        compound.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        compound.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.SEPARATE_CHILDREN);
        compound.setProperty(CoreOptions.DIRECTION, toElkDirection(direction));
        compound.setProperty(CoreOptions.SPACING_NODE_NODE, (double) cfg.nodeSpacing());
        compound.setProperty(CoreOptions.SPACING_EDGE_NODE, (double) cfg.nodeSpacing());
        FlowchartLayoutResult.LabelSize label = labelSizes.get(sg.getId());
        int labelHeight = label != null ? label.height() : 0;
        int labelWidth = label != null ? label.width() : 0;
        // The title backdrop masks crossing edges, so only reserve space above the child nodes.
        compound.setProperty(
            CoreOptions.PADDING,
            new ElkPadding(
                Math.max(SUBGRAPH_PADDING, labelHeight),
                SUBGRAPH_PADDING,
                SUBGRAPH_PADDING,
                SUBGRAPH_PADDING));
        compound.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.of(SizeConstraint.MINIMUM_SIZE));
        compound.setProperty(CoreOptions.NODE_SIZE_MINIMUM, new KVector(labelWidth + 8, 0));
        compoundMap.put(sg.getId(), compound);

        for (FlowchartSubgraph child : sg.getChildren()) {
            createCompounds(child, compound, compoundMap, direction, cfg, labelSizes);
        }
    }

    private static ElkNode nodeParent(String nodeId, Map<String, FlowchartSubgraph> nodeToSubgraph,
        Set<String> compoundSgIds, Map<String, String> parentSgMap, ElkNode root, Map<String, ElkNode> compoundMap,
        Map<String, FlowchartSubgraph> subgraphById) {
        FlowchartSubgraph sg = nodeToSubgraph.get(nodeId);
        while (sg != null) {
            if (compoundSgIds.contains(sg.getId())) {
                ElkNode compound = compoundMap.get(sg.getId());
                if (compound != null) return compound;
            }
            String parentId = parentSgMap.get(sg.getId());
            sg = parentId != null ? subgraphById.get(parentId) : null;
        }
        return root;
    }

    private static void collectPositions(ElkNode node, double offsetX, double offsetY, int padding,
        Map<String, FlowchartLayoutResult.NodePosition> positions, Map<String, ElkNode> elkNodeMap) {
        boolean isCompound = node.getIdentifier() != null && node.getIdentifier()
            .startsWith("__sg_");
        double absX = offsetX + node.getX();
        double absY = offsetY + node.getY();

        if (!isCompound && node.getIdentifier() != null && elkNodeMap.containsKey(node.getIdentifier())) {
            positions.put(
                node.getIdentifier(),
                new FlowchartLayoutResult.NodePosition(
                    padding + (int) Math.round(absX),
                    padding + (int) Math.round(absY),
                    Math.max(1, (int) Math.round(node.getWidth())),
                    Math.max(1, (int) Math.round(node.getHeight()))));
        }

        for (ElkNode child : node.getChildren()) {
            collectPositions(child, absX, absY, padding, positions, elkNodeMap);
        }
    }

    private static Map<String, FlowchartSubgraph> buildNodeToSubgraphMap(List<FlowchartSubgraph> subgraphs) {
        Map<String, FlowchartSubgraph> result = new LinkedHashMap<>();
        buildNodeToSubgraphMapRec(subgraphs, result);
        return result;
    }

    private static void buildNodeToSubgraphMapRec(List<FlowchartSubgraph> subgraphs,
        Map<String, FlowchartSubgraph> result) {
        for (FlowchartSubgraph sg : subgraphs) {
            buildNodeToSubgraphMapRec(sg.getChildren(), result);
            for (String nodeId : sg.getNodeIds()) {
                result.putIfAbsent(nodeId, sg);
            }
        }
    }

    private static Direction toElkDirection(FlowchartDirection dir) {
        if (dir == null) return Direction.DOWN;
        return switch (dir) {
            case LR -> Direction.RIGHT;
            case RL -> Direction.LEFT;
            case BT -> Direction.UP;
            default -> Direction.DOWN;
        };
    }

    private record SplitChain(String sourceId, String targetId, @Nullable String edgeId, List<ElkPort> srcPorts,
        List<ElkPort> tgtPorts, ElkEdge externalEdge, List<ElkEdge> srcEdges, List<ElkEdge> tgtEdges) {}

    private record DummyChain(String sourceId, String targetId, @Nullable String edgeId, List<ElkEdge> chainEdges) {}
}
