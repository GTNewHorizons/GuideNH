package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.argbToRgba;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.MermaidNodeRenderer;
import com.hfstudio.guidenh.guide.document.block.shapes.FlowchartShapes;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartEdge;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartSubgraph;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapNode;

public class GuideSiteMermaidRenderer {

    private GuideSiteMermaidRenderer() {}

    private static final int SUBGRAPH_LABEL_BASELINE = 12;

    private static final int MM_DEFAULT_ACCENT = ColorUtils.ARGB_FF7AA2F7.getColor();

    private static class MmLayoutNode {

        private final MindmapNode source;
        @Nullable
        private final String parentId;
        private final boolean isRoot;
        @Nullable
        private final String htmlContent;
        private final String[] lines;
        private final @Nullable String badge;
        private final List<MmLayoutNode> children = new ArrayList<>();

        private MmLayoutNode(MindmapNode source, @Nullable String parentId, boolean isRoot,
            @Nullable String htmlContent) {
            this.source = source;
            this.parentId = parentId;
            this.isRoot = isRoot;
            this.htmlContent = htmlContent;
            String text = source.getText() != null ? source.getText() : "";
            this.lines = text.isEmpty() ? new String[] { "" } : text.split("\n");
            this.badge = simplifyMermaidIcon(source.getIcon());
        }
    }

    public static String renderMermaidTree(MindmapDocument doc) {
        return renderMermaidTree(doc, new LinkedHashMap<>());
    }

    public static String renderMermaidTree(MindmapDocument doc, Map<String, String> nodeHtmlById) {
        if (doc == null || doc.getRoot() == null) {
            return "<div class=\"guide-mermaid-pan\" data-guide-pannable>"
                + "<div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>"
                + "<svg class=\"guide-mermaid-canvas\" width=\"100\" height=\"40\"></svg>"
                + "<div class=\"guide-mermaid-node-layer\"></div>"
                + "<p class=\"guide-diagram-empty\">No Mermaid nodes were found.</p>"
                + "</div></div>";
        }
        MmLayoutNode root = buildMmLayout(
            doc.getRoot(),
            null,
            true,
            nodeHtmlById != null ? nodeHtmlById : new LinkedHashMap<>());
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"guide-mermaid-pan\" data-guide-pannable><div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>");
        html.append(
            "<svg class=\"guide-mermaid-canvas\" xmlns=\"http://www.w3.org/2000/svg\" aria-hidden=\"true\"></svg>");
        html.append("<div class=\"guide-mermaid-node-layer\">");
        renderMmNodes(html, root);
        html.append("</div></div></div>");
        return html.toString();
    }

    public static String renderFlowchart(FlowchartDocument doc) {
        return renderFlowchart(doc, new LinkedHashMap<>());
    }

    public static String renderFlowchart(FlowchartDocument doc, Map<String, String> nodeHtmlById) {
        if (doc == null || doc.getNodes()
            .isEmpty()) {
            return "<div class=\"guide-mermaid-pan\" data-guide-pannable>"
                + "<div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>"
                + "<svg class=\"guide-mermaid-canvas\" width=\"100\" height=\"40\"></svg>"
                + "<p class=\"guide-diagram-empty\">No Mermaid nodes were found.</p>"
                + "</div></div>";
        }
        if (nodeHtmlById == null) nodeHtmlById = Map.of();
        String rootNodeId = doc.getNodeOrder()
            .isEmpty() ? null
                : doc.getNodeOrder()
                    .get(0);
        Map<String, FlowchartLayoutResult.NodeMinSize> minSizes = new LinkedHashMap<>();
        for (FlowchartNode node : doc.getNodes()
            .values()) {
            String htmlContent = nodeHtmlById.get(node.getId());
            boolean hasHtmlContent = htmlContent != null && !htmlContent.trim()
                .isEmpty();
            PlainLabel plainLabel = hasHtmlContent ? null : measurePlainLabel(node.getLabel());
            int[] textExtent = hasHtmlContent ? estimateTextExtent(htmlContent) : plainContentExtent(node, plainLabel);
            int textWidth = textExtent[0];
            int textHeight = textExtent[1];
            MermaidNodeShape shape = node.getShape() != null ? node.getShape() : MermaidNodeShape.SQUARE;
            boolean isRoot = node.getId()
                .equals(rootNodeId);
            int padX = isRoot ? 15 : 10;
            int padY = isRoot ? 9 : 6;
            LytRect minRect = FlowchartShapes.minNodeRect(shape, textWidth, textHeight, padX, padY);
            int minW = minRect.width();
            int minH = minRect.height();
            if (FlowchartShapes.isShapeClipped(shape)) {
                minW += NODE_EDGE_MARGIN * 2;
                minH += NODE_EDGE_MARGIN * 2;
            }
            minSizes.put(node.getId(), new FlowchartLayoutResult.NodeMinSize(minW, minH));
        }
        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(doc.getLayoutMode());
        FlowchartLayoutResult layout = strategy
            .layout(doc, minSizes, label -> new FlowchartLayoutResult.LabelSize(estimateTextWidth(label), 14));
        int w = layout.getWidth() + 40;
        int h = layout.getHeight() + 40;
        int padding = 20;

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mermaid-pan\" data-guide-pannable>\n");
        html.append("<div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>\n");
        html.append("<svg class=\"guide-mermaid-canvas\" viewBox=\"0 0 ")
            .append(w)
            .append(" ")
            .append(h)
            .append("\"")
            .append(" width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\"")
            .append(" xmlns=\"http://www.w3.org/2000/svg\">\n");
        html.append("<defs>\n");
        appendArrowheadMarkers(html);
        html.append("</defs>\n");
        renderFlowchartSubgraphs(html, doc, layout, padding);
        renderFlowchartEdges(html, doc, layout, padding);
        renderFlowchartEdgeLabels(html, layout, padding);
        renderFlowchartNodes(html, doc, layout, nodeHtmlById, padding, rootNodeId);
        renderFlowchartSubgraphLabels(html, doc, layout, padding);
        html.append("</svg>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }

    private static int estimateTextWidth(CharSequence text) {
        if (text == null || text.isEmpty()) return 0;
        int w = 0;
        for (int i = 0; i < text.length();) {
            int codePoint = Character.codePointAt(text, i);
            w += estimatedCharWidth(codePoint);
            i += Character.charCount(codePoint);
        }
        return Math.max(w + 8, 28);
    }

    private static final int MAX_TEXT_WRAP_WIDTH = 150;

    private static int[] estimateTextExtent(String text) {
        if (text == null || text.isEmpty()) {
            return new int[] { 20, 14 };
        }
        String[] lines = text.split("\n");
        int maxWidth = 0;
        int totalLines = 0;
        for (String line : lines) {
            int lineW = estimateTextWidth(line);
            if (lineW > MAX_TEXT_WRAP_WIDTH) {
                int wraps = (lineW + MAX_TEXT_WRAP_WIDTH - 1) / MAX_TEXT_WRAP_WIDTH;
                totalLines += wraps;
                maxWidth = Math.max(maxWidth, MAX_TEXT_WRAP_WIDTH);
            } else {
                totalLines += 1;
                maxWidth = Math.max(maxWidth, lineW);
            }
        }
        int height = Math.max(14, totalLines * 14);
        return new int[] { maxWidth, height };
    }

    private record PlainLabel(List<String> lines, int width, int height) {}

    private static PlainLabel measurePlainLabel(String text) {
        if (text == null || text.isEmpty()) return new PlainLabel(List.of(), 20, 14);

        List<String> lines = new ArrayList<>();
        for (String sourceLine : text.split("\n", -1)) {
            if (sourceLine.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            int lineWidth = 8;
            // Iterate by code point so supplementary characters (e.g. emoji) are never split.
            for (int i = 0; i < sourceLine.length();) {
                int codePoint = sourceLine.codePointAt(i);
                i += Character.charCount(codePoint);
                int charWidth = estimatedCharWidth(codePoint);
                if (line.length() > 0 && lineWidth + charWidth > MAX_TEXT_WRAP_WIDTH) {
                    lines.add(line.toString());
                    line.setLength(0);
                    lineWidth = 8;
                }
                line.appendCodePoint(codePoint);
                lineWidth += charWidth;
            }
            lines.add(line.toString());
        }

        int width = 0;
        for (String line : lines) width = Math.max(width, estimateTextWidth(line));
        return new PlainLabel(lines, Math.max(width, 20), Math.max(14, lines.size() * 14));
    }

    private static int estimatedCharWidth(int c) {
        if ("iIl!|;,.:'\\\"".indexOf(c) >= 0) return 4;
        if ("mwMW".indexOf(c) >= 0) return 9;
        if (" \t".indexOf(c) >= 0) return 4;
        return 7;
    }

    private static int[] plainContentExtent(FlowchartNode node, PlainLabel label) {
        int width = label != null ? label.width() : 20;
        int height = label != null ? label.height() : 14;
        String badgeText = node.getIcon() != null ? MermaidNodeRenderer.simplifyIcon(node.getIcon()) : null;
        if (badgeText != null) {
            width = Math.max(width, Math.max(badgeText.length() * 7 + 8, 20));
            height += 18 + 4;
        }
        return new int[] { width, height };
    }

    private static String escapedLines(List<String> lines) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) result.append("<br/>");
            result.append(escapeHtml(lines.get(i)));
        }
        return result.toString();
    }

    private static void appendArrowheadMarkers(StringBuilder sb) {
        sb.append("<marker id=\"arrow-triangle\" viewBox=\"0 0 8 6\" refX=\"8\" refY=\"3\"")
            .append(" markerWidth=\"8\" markerHeight=\"6\" markerUnits=\"userSpaceOnUse\" orient=\"auto\">\n");
        sb.append("<path d=\"M 0 0 L 8 3 L 0 6 z\" fill=\"currentColor\"/>\n");
        sb.append("</marker>\n");

        sb.append("<marker id=\"arrow-circle\" viewBox=\"0 0 10 10\" refX=\"10\" refY=\"5\"")
            .append(" markerWidth=\"10\" markerHeight=\"10\" markerUnits=\"userSpaceOnUse\" orient=\"auto\">\n");
        sb.append("<circle cx=\"5\" cy=\"5\" r=\"5\" fill=\"currentColor\"/>\n");
        sb.append("</marker>\n");

        sb.append("<marker id=\"arrow-cross\" viewBox=\"0 0 8 8\" refX=\"6\" refY=\"4\"")
            .append(" markerWidth=\"8\" markerHeight=\"8\" markerUnits=\"userSpaceOnUse\" orient=\"auto\">\n");
        sb.append("<path d=\"M 1 1 L 7 7 M 7 1 L 1 7\" stroke=\"currentColor\" stroke-width=\"1.5\"/>\n");
        sb.append("</marker>\n");
    }

    private static void renderFlowchartSubgraphs(StringBuilder sb, FlowchartDocument doc, FlowchartLayoutResult layout,
        int padding) {
        for (FlowchartSubgraph sg : doc.getSubgraphs()) {
            renderSubgraphRecursive(sb, sg, layout, padding, 0);
        }
    }

    private static void renderSubgraphRecursive(StringBuilder sb, FlowchartSubgraph sg, FlowchartLayoutResult layout,
        int padding, int depth) {
        FlowchartLayoutResult.NodePosition position = layout.getSubgraphPositions()
            .get(sg.getId());
        if (position == null) return;

        int x = position.getX() + padding;
        int y = position.getY() + padding;
        int w = position.getWidth();
        int h = position.getHeight();

        String bg = SUBGRAPH_BG[depth % SUBGRAPH_BG.length];
        String border = SUBGRAPH_BORDER[depth % SUBGRAPH_BORDER.length];

        sb.append("<rect class=\"guide-flowchart-subgraph\" x=\"")
            .append(x)
            .append("\" y=\"")
            .append(y)
            .append("\" width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\" rx=\"0\" ry=\"0\"")
            .append(" fill=\"")
            .append(bg)
            .append("\"")
            .append(" stroke=\"")
            .append(border)
            .append("\"")
            .append(" stroke-width=\"1.5\"/>\n");

        for (FlowchartSubgraph child : sg.getChildren()) {
            renderSubgraphRecursive(sb, child, layout, padding, depth + 1);
        }
    }

    private static void renderFlowchartSubgraphLabels(StringBuilder sb, FlowchartDocument doc,
        FlowchartLayoutResult layout, int padding) {
        for (FlowchartSubgraph sg : doc.getSubgraphs()) {
            renderSubgraphLabelRecursive(sb, sg, layout, padding, 0);
        }
    }

    private static void renderSubgraphLabelRecursive(StringBuilder sb, FlowchartSubgraph sg,
        FlowchartLayoutResult layout, int padding, int depth) {
        String label = sg.getLabel();
        FlowchartLayoutResult.NodePosition position = layout.getSubgraphLabelPositions()
            .get(sg.getId());
        if (label != null && !label.isEmpty() && position != null) {
            int x = position.getX() + padding;
            int y = position.getY() + padding;
            int backgroundPadding = 2;
            String background = SUBGRAPH_BG_OPAQUE[depth % SUBGRAPH_BG_OPAQUE.length];
            sb.append("<rect class=\"guide-flowchart-subgraph-label-background\" x=\"")
                .append(x - backgroundPadding)
                .append("\" y=\"")
                .append(y - backgroundPadding)
                .append("\" width=\"")
                .append(position.getWidth() + backgroundPadding * 2)
                .append("\" height=\"")
                .append(position.getHeight() + backgroundPadding * 2)
                .append("\" fill=\"")
                .append(background)
                .append("\" stroke=\"none\"/>\n");
            sb.append("<text x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y + SUBGRAPH_LABEL_BASELINE)
                .append("\" font-size=\"11\" font-weight=\"bold\" fill=\"#C8CDD6\"")
                .append(" font-family=\"inherit\">")
                .append(escapeHtml(label))
                .append("</text>\n");
        }
        for (FlowchartSubgraph child : sg.getChildren()) {
            renderSubgraphLabelRecursive(sb, child, layout, padding, depth + 1);
        }
    }

    private static void renderFlowchartEdges(StringBuilder sb, FlowchartDocument doc, FlowchartLayoutResult layout,
        int padding) {
        for (FlowchartLayoutResult.EdgePath edgePath : layout.getEdgePaths()) {
            FlowchartEdge flowEdge = lookupFlowEdge(doc, edgePath);
            MermaidEdgeStyle style = resolveEdgeStyle(flowEdge);
            if (style == MermaidEdgeStyle.INVISIBLE) continue;

            boolean arrowFwd = flowEdge == null || flowEdge.isArrowFwd();
            boolean arrowRev = flowEdge != null && flowEdge.isArrowRev();
            MermaidArrowHead fwdHead = resolveArrowHead(true, flowEdge);
            MermaidArrowHead revHead = resolveArrowHead(false, flowEdge);

            List<FlowchartLayoutResult.Point> points = edgePath.getPoints();
            if (points.size() < 2) continue;

            // Compute shape-aware intersection for source and target endpoints (null if not clipped / unchanged)
            FlowchartLayoutResult.Point srcBoundary = intersectEdgeEndpoint(
                doc,
                layout,
                edgePath.getFromId(),
                points.get(0));
            FlowchartLayoutResult.Point tgtBoundary = intersectEdgeEndpoint(
                doc,
                layout,
                edgePath.getToId(),
                points.get(points.size() - 1));

            // Build polyline: for clipped shapes, add boundary→ELK-endpoint margin gap
            List<FlowchartLayoutResult.Point> edgePts = new ArrayList<>();
            if (srcBoundary != null) {
                edgePts.add(srcBoundary);
                edgePts.add(points.get(0));
            } else {
                edgePts.add(points.get(0));
            }
            for (int i = 1; i < points.size() - 1; i++) {
                edgePts.add(points.get(i));
            }
            if (tgtBoundary != null) {
                edgePts.add(points.get(points.size() - 1));
                edgePts.add(tgtBoundary);
            } else {
                edgePts.add(points.get(points.size() - 1));
            }

            String strokeColor = DEFAULT_EDGE_COLOR;
            int strokeWidth = style == MermaidEdgeStyle.THICK ? 4 : 2;
            if (flowEdge != null) {
                String edgeStyles = flowEdge.getStyleOverride();
                if (edgeStyles != null) {
                    String stroke = getStyleProperty(edgeStyles, "stroke");
                    if (stroke != null) strokeColor = stroke;
                    String width = getStyleProperty(edgeStyles, "stroke-width");
                    if (width != null) {
                        try {
                            strokeWidth = Math.max(
                                1,
                                Integer.parseInt(
                                    width.replace("px", "")
                                        .trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            String strokeDash = switch (style) {
                case DASHED -> "8,1";
                case DOTTED -> "1,3";
                default -> null;
            };

            sb.append("<polyline points=\"");
            for (int i = 0; i < edgePts.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(
                    edgePts.get(i)
                        .getX() + padding)
                    .append(",")
                    .append(
                        edgePts.get(i)
                            .getY() + padding);
            }
            sb.append("\"");
            sb.append(" color=\"")
                .append(strokeColor)
                .append("\"");
            sb.append(" stroke=\"")
                .append(strokeColor)
                .append("\"");
            sb.append(" stroke-width=\"")
                .append(strokeWidth)
                .append("\"");
            sb.append(" fill=\"none\"");
            if (strokeDash != null) {
                sb.append(" stroke-dasharray=\"")
                    .append(strokeDash)
                    .append("\"");
            }
            if (arrowFwd && fwdHead != MermaidArrowHead.NONE) {
                sb.append(" marker-end=\"url(#arrow-")
                    .append(arrowHeadId(fwdHead))
                    .append(")\"");
            }
            if (arrowRev && revHead != MermaidArrowHead.NONE) {
                sb.append(" marker-start=\"url(#arrow-")
                    .append(arrowHeadId(revHead))
                    .append(")\"");
            }
            sb.append("/>\n");

        }
    }

    private static void renderFlowchartEdgeLabels(StringBuilder sb, FlowchartLayoutResult layout, int padding) {
        for (var entry : layout.getEdgeLabelPositions()
            .entrySet()) {
            FlowchartEdge edge = entry.getKey();
            FlowchartLayoutResult.NodePosition position = entry.getValue();

            int x = position.getX() + padding;
            int y = position.getY() + padding;
            int w = position.getWidth();
            int h = position.getHeight();
            int cx = x + w / 2;
            int cy = y + h / 2;
            sb.append("<rect x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y)
                .append("\" width=\"")
                .append(w)
                .append("\" height=\"")
                .append(h)
                .append("\" rx=\"0\"")
                .append(" fill=\"")
                .append(argbToRgba(ColorUtils.ARGB_CC0C1117.getColor()))
                .append("\" stroke=\"rgba(180,180,200,0.3)\"")
                .append(" stroke-width=\"1\"/>\n");
            sb.append("<text x=\"")
                .append(cx)
                .append("\" y=\"")
                .append(cy + 4)
                .append("\" font-size=\"11\" fill=\"rgba(200,200,220,0.9)\"")
                .append(" text-anchor=\"middle\">")
                .append(escapeHtml(edge.getLabel()))
                .append("</text>\n");
        }
    }

    private static final int NODE_EDGE_MARGIN = 12;
    private static final String DEFAULT_EDGE_COLOR = "#5D6C7C";
    private static final String[] SUBGRAPH_BG = { argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_PURPLE.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_VIOLET.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_DARK.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_GOLD.getColor()), };
    private static final String[] SUBGRAPH_BG_OPAQUE = {
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_PURPLE.getColor() | 0xFF000000),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_VIOLET.getColor() | 0xFF000000),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_DARK.getColor() | 0xFF000000),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_GOLD.getColor() | 0xFF000000), };
    private static final String[] SUBGRAPH_BORDER = { argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BORDER_BLUE.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BORDER_GREEN.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BORDER_OLIVE.getColor()),
        argbToRgba(ColorUtils.MERMAID_SUBGRAPH_BORDER_TEAL.getColor()), };

    @Nullable
    private static FlowchartLayoutResult.Point intersectEdgeEndpoint(FlowchartDocument doc,
        FlowchartLayoutResult layout, String nodeId, FlowchartLayoutResult.Point boundaryPoint) {
        if (nodeId == null || doc == null) return null;
        FlowchartNode node = doc.getNodes()
            .get(nodeId);
        FlowchartLayoutResult.NodePosition pos = layout.getNodePositions()
            .get(nodeId);
        if (node == null || pos == null || node.getShape() == null) return null;
        if (!FlowchartShapes.isShapeClipped(node.getShape())) return null;
        int margin = NODE_EDGE_MARGIN;
        LytRect nodeRect = new LytRect(
            pos.getX() + margin,
            pos.getY() + margin,
            Math.max(pos.getWidth() - margin * 2, 1),
            Math.max(pos.getHeight() - margin * 2, 1));
        FlowchartLayoutResult.Point intersect = FlowchartShapes
            .edgeIntersect(nodeRect, node.getShape(), boundaryPoint.getX(), boundaryPoint.getY());
        return intersect != null
            && (intersect.getX() != boundaryPoint.getX() || intersect.getY() != boundaryPoint.getY()) ? intersect
                : null;
    }

    private static FlowchartEdge lookupFlowEdge(FlowchartDocument doc, FlowchartLayoutResult.EdgePath edgePath) {
        String edgeId = edgePath.getEdgeId();
        if (edgeId != null) {
            for (FlowchartEdge e : doc.getEdges()) {
                if (edgeId.equals(e.getEdgeId())) return e;
            }
        }
        for (FlowchartEdge e : doc.getEdges()) {
            if (e.getFrom()
                .equals(edgePath.getFromId())
                && e.getTo()
                    .equals(edgePath.getToId())) {
                return e;
            }
        }
        return null;
    }

    @Nullable
    private static String getStyleProperty(@Nullable String styleOverride, String property) {
        if (styleOverride == null) return null;
        String last = null;
        for (String part : styleOverride.split(",")) {
            int colon = part.indexOf(':');
            if (colon > 0 && part.substring(0, colon)
                .trim()
                .equalsIgnoreCase(property)) {
                last = part.substring(colon + 1)
                    .trim();
            }
        }
        return last;
    }

    private static MermaidEdgeStyle resolveEdgeStyle(FlowchartEdge edge) {
        return edge != null ? edge.getStyle() : MermaidEdgeStyle.SOLID;
    }

    private static MermaidArrowHead resolveArrowHead(boolean isForward, FlowchartEdge edge) {
        if (edge == null) return MermaidArrowHead.TRIANGLE;
        return isForward ? edge.getForwardHead() : edge.getReverseHead();
    }

    private static String arrowHeadId(MermaidArrowHead head) {
        return switch (head) {
            case TRIANGLE -> "triangle";
            case CIRCLE -> "circle";
            case CROSS -> "cross";
            default -> "triangle";
        };
    }

    private static void renderFlowchartNodes(StringBuilder sb, FlowchartDocument doc, FlowchartLayoutResult layout,
        Map<String, String> nodeHtmlById, int padding, @Nullable String rootNodeId) {

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            FlowchartLayoutResult.NodePosition pos = entry.getValue();
            FlowchartNode node = doc.getNodes()
                .get(nodeId);
            if (node == null) continue;

            boolean isRoot = nodeId.equals(rootNodeId);
            MermaidNodeShape shape = node.getShape() != null ? node.getShape() : MermaidNodeShape.SQUARE;

            int sx = pos.getX() + padding;
            int sy = pos.getY() + padding;
            int sw = pos.getWidth();
            int sh = pos.getHeight();

            int rectX, rectY, rectW, rectH;
            if (FlowchartShapes.isShapeClipped(shape)) {
                int m = NODE_EDGE_MARGIN;
                rectX = sx + m;
                rectY = sy + m;
                rectW = sw - m * 2;
                rectH = sh - m * 2;
            } else {
                rectX = sx;
                rectY = sy;
                rectW = sw;
                rectH = sh;
            }

            var colors = MermaidNodeRenderer.resolveNodeColors(node.getClasses(), shape, isRoot);
            String bgColor = argbToRgba(colors.background());
            String borderColor = argbToRgba(colors.border());
            int accent = colors.accent();

            String nodeStyles = node.getStyleOverride();
            if (nodeStyles != null) {
                String fill = getStyleProperty(nodeStyles, "fill");
                String stroke = getStyleProperty(nodeStyles, "stroke");
                if (fill != null) bgColor = fill;
                if (stroke != null) borderColor = stroke;
            }

            sb.append(FlowchartShapes.renderSvg(shape, rectX, rectY, rectW, rectH, bgColor, borderColor))
                .append("\n");

            if (accent != MermaidNodeRenderer.DEFAULT_ACCENT && FlowchartShapes.hasAccentBar(shape)) {
                sb.append("<rect x=\"")
                    .append(rectX)
                    .append("\" y=\"")
                    .append(rectY)
                    .append("\" width=\"3\" height=\"")
                    .append(rectH)
                    .append("\" fill=\"")
                    .append(argbToRgba(accent))
                    .append("\" rx=\"0\"/>\n");
            }

            int padX = isRoot ? 15 : 10;
            int padY = isRoot ? 9 : 6;
            int contentW = rectW - padX * 2;
            int contentH = rectH - padY * 2;
            String htmlContent = nodeHtmlById.get(nodeId);
            boolean hasHtmlContent = htmlContent != null && !htmlContent.trim()
                .isEmpty();
            PlainLabel plainLabel = hasHtmlContent ? null : measurePlainLabel(node.getLabel());
            int[] requestedContent = hasHtmlContent ? new int[] { contentW, contentH }
                : plainContentExtent(node, plainLabel);
            LytRect contentArea = FlowchartShapes.contentBounds(
                new LytRect(rectX, rectY, rectW, rectH),
                shape,
                requestedContent[0],
                requestedContent[1],
                padX,
                padY);
            int textY = contentArea.y();

            String textColor = isRoot ? "#F1F6FB" : "#D7DEE7";
            String fontWeight = isRoot ? "bold" : "normal";

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = Math.max(badgeText.length() * 7 + 8, 20);
                    int badgeHeight = 18;
                    sb.append("<rect x=\"")
                        .append(contentArea.x())
                        .append("\" y=\"")
                        .append(textY)
                        .append("\" width=\"")
                        .append(badgeWidth)
                        .append("\" height=\"")
                        .append(badgeHeight)
                        .append("\" rx=\"0\" ry=\"0\"")
                        .append(" fill=\"")
                        .append(argbToRgba(MermaidNodeRenderer.BADGE_BACKGROUND))
                        .append("\" stroke=\"")
                        .append(argbToRgba(MermaidNodeRenderer.BADGE_BORDER))
                        .append("\" stroke-width=\"1\"/>\n");
                    sb.append("<text x=\"")
                        .append(contentArea.x() + 4)
                        .append("\" y=\"")
                        .append(textY + 13)
                        .append("\" font-size=\"10\" fill=\"")
                        .append(argbToRgba(ColorUtils.CHART_LABEL.getColor()))
                        .append("\" font-family=\"inherit\">")
                        .append(escapeHtml(badgeText))
                        .append("</text>\n");
                    textY += badgeHeight + 4;
                }
            }

            int visibleWidth = contentArea.width();
            int visibleHeight = contentArea.height();
            if (hasHtmlContent) {
                sb.append("<foreignObject x=\"")
                    .append(contentArea.x())
                    .append("\" y=\"")
                    .append(textY)
                    .append("\" width=\"")
                    .append(Math.max(1, visibleWidth))
                    .append("\" height=\"")
                    .append(Math.max(1, visibleHeight))
                    .append("\">")
                    .append("<div xmlns=\"http://www.w3.org/1999/xhtml\" class=\"guide-flowchart-html-content\"")
                    .append(" style=\"display:flex;align-items:center;justify-content:center;")
                    .append("width:100%;min-height:100%;overflow:visible;")
                    .append("font-size:12px;font-weight:")
                    .append(fontWeight)
                    .append(";")
                    .append("color:")
                    .append(textColor)
                    .append(";")
                    .append("text-align:center;white-space:normal;overflow-wrap:break-word;")
                    .append("word-break:break-word;\">")
                    .append(htmlContent)
                    .append("</div>")
                    .append("</foreignObject>\n");
            } else {
                String label = node.getLabel();
                if (label != null && !label.isEmpty()) {
                    int textAreaH = contentArea.y() + visibleHeight - textY;
                    sb.append("<foreignObject x=\"")
                        .append(contentArea.x())
                        .append("\" y=\"")
                        .append(textY)
                        .append("\" width=\"")
                        .append(Math.max(1, visibleWidth))
                        .append("\" height=\"")
                        .append(Math.max(1, textAreaH))
                        .append("\">")
                        .append("<div xmlns=\"http://www.w3.org/1999/xhtml\"")
                        .append(" style=\"display:flex;align-items:center;justify-content:center;")
                        .append("width:100%;min-height:100%;overflow:visible;")
                        .append("font-size:12px;font-weight:")
                        .append(fontWeight)
                        .append(";")
                        .append("color:")
                        .append(textColor)
                        .append(";")
                        .append("text-align:center;white-space:nowrap;overflow-wrap:normal;")
                        .append("word-break:normal;\">")
                        .append("<span>")
                        .append(escapedLines(plainLabel.lines()))
                        .append("</span>")
                        .append("</div>")
                        .append("</foreignObject>\n");
                }
            }
        }
    }

    private static MmLayoutNode buildMmLayout(MindmapNode source, @Nullable String parentId, boolean isRoot,
        Map<String, String> nodeHtmlById) {
        MmLayoutNode node = new MmLayoutNode(source, parentId, isRoot, nodeHtmlById.get(source.getId()));
        for (MindmapNode child : source.getChildren()) {
            node.children.add(buildMmLayout(child, source.getId(), false, nodeHtmlById));
        }
        return node;
    }

    private static void renderMmNodes(StringBuilder html, MmLayoutNode node) {
        html.append("<article class=\"guide-mermaid-node guide-mermaid-shape-")
            .append(escapeShapeClass(node.source.getShape()))
            .append(node.isRoot ? " guide-mermaid-node-root" : "")
            .append("\" data-node-id=\"")
            .append(escapeHtml(node.source.getId()))
            .append("\"");
        if (node.parentId != null && !node.parentId.isEmpty()) {
            html.append(" data-parent-id=\"")
                .append(escapeHtml(node.parentId))
                .append("\"");
        }
        html.append(" data-accent=\"")
            .append(argbToRgba(resolveMmAccent(node)))
            .append("\"");
        html.append(">");
        html.append("<span class=\"guide-mermaid-node-accent\"></span>");
        if (node.badge != null && !node.badge.isEmpty()) {
            html.append("<div class=\"guide-mermaid-node-badge\">")
                .append(escapeHtml(node.badge))
                .append("</div>");
        }
        html.append("<div class=\"guide-mermaid-node-body\">");
        if (node.htmlContent != null && !node.htmlContent.trim()
            .isEmpty()) {
            html.append(node.htmlContent);
        } else {
            appendMmFallbackLines(html, node);
        }
        html.append("</div></article>");
        for (MmLayoutNode child : node.children) {
            renderMmNodes(html, child);
        }
    }

    private static void appendMmFallbackLines(StringBuilder html, MmLayoutNode node) {
        for (String line : node.lines) {
            html.append("<div class=\"guide-mermaid-node-line\">")
                .append(escapeHtml(line))
                .append("</div>");
        }
    }

    private static String escapeShapeClass(@Nullable MermaidNodeShape shape) {
        return switch (shape != null ? shape : MermaidNodeShape.DEFAULT) {
            case ROUNDED -> "rounded";
            case CIRCLE -> "circle";
            case HEXAGON -> "hexagon";
            case CLOUD -> "cloud";
            case BANG -> "bang";
            case SQUARE -> "square";
            default -> "default";
        };
    }

    private static @Nullable String simplifyMermaidIcon(@Nullable String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }
        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? trimmed : leaf;
    }

    private static int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    private static int resolveMmAccent(MmLayoutNode node) {
        int accent = MM_DEFAULT_ACCENT;
        for (String className : node.source.getClasses()) {
            String lower = className.toLowerCase(Locale.ROOT);
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = ColorUtils.ARGB_FFF7768E.getColor();
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = ColorUtils.ARGB_FF9ECE6A.getColor();
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = ColorUtils.ARGB_FFE0AF68.getColor();
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = ColorUtils.ARGB_FF8B949E.getColor();
            }
        }
        return switch (node.source.getShape()) {
            case CIRCLE -> ColorUtils.ARGB_FF7DCFFF.getColor();
            case HEXAGON -> ColorUtils.ARGB_FFE0AF68.getColor();
            case CLOUD -> ColorUtils.ARGB_FF73DACA.getColor();
            case BANG -> ColorUtils.ARGB_FFF7768E.getColor();
            default -> accent;
        };
    }
}
