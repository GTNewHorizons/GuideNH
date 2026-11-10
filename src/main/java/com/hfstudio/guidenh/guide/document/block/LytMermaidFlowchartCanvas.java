package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.shapes.FlowchartShapes;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartEdge;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.EdgePath;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.LabelSize;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodeMinSize;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodePosition;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartSubgraph;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class LytMermaidFlowchartCanvas extends LytMermaidCanvas<LytMermaidFlowchartCanvas> implements DebugComponent {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 120;
    private static final int MAX_HEIGHT = 320;
    private static final int CONNECTOR_THICKNESS = 2;
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int NODE_EDGE_MARGIN = 12;
    private static final int ICON_GAP_Y = 4;
    private static final int BADGE_PADDING_X = 4;
    private static final int BADGE_PADDING_Y = 2;
    private static final ConstantColor NODE_TEXT = new ConstantColor(ColorUtils.ARGB_FFD7DEE7.getColor());
    private static final ConstantColor ROOT_TEXT_COLOR = new ConstantColor(ColorUtils.ARGB_FFF1F6FB.getColor());
    private static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(ColorUtils.CHART_LABEL.getColor());
    private static final ConstantColor EDGE_COLOR = new ConstantColor(ColorUtils.ARGB_FF5D6C7C.getColor());
    private static final ConstantColor[] SUBGRAPH_BG = {
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_PURPLE.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_VIOLET.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_DARK.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BACKGROUND_GOLD.getColor()), };
    private static final ConstantColor[] SUBGRAPH_BORDER = {
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BORDER_BLUE.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BORDER_GREEN.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BORDER_OLIVE.getColor()),
        new ConstantColor(ColorUtils.MERMAID_SUBGRAPH_BORDER_TEAL.getColor()), };

    private static final ResolvedTextStyle NODE_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        NODE_TEXT,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);
    private static final ResolvedTextStyle ROOT_TEXT_STYLE = new ResolvedTextStyle(
        1f,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        ROOT_TEXT_COLOR,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);
    private static final ResolvedTextStyle ICON_TEXT_STYLE = new ResolvedTextStyle(
        0.85f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        ICON_TEXT_COLOR,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);

    private final FlowchartDocument document;
    private final Map<String, NodeContentLayout> nodeContentLayouts = new LinkedHashMap<>();
    private final Map<String, NodeMinSize> nodeContentSizes = new LinkedHashMap<>();
    private final Map<String, NodeLabelLayout> nodeLabelLayouts = new LinkedHashMap<>();
    private FlowchartLayoutResult layout;

    public LytMermaidFlowchartCanvas(FlowchartDocument document, Map<String, LytBlock> nodeContentBlocks) {
        this.document = document;
        initNodeContentBlocks(nodeContentBlocks);
    }

    @Override
    public int canvasPadding() {
        return CANVAS_PADDING;
    }

    @Override
    public int contentWidth() {
        return layout != null ? layout.getWidth() : 0;
    }

    @Override
    public int contentHeight() {
        return layout != null ? layout.getHeight() : 0;
    }

    @Override
    public int contentOriginX() {
        return 0;
    }

    @Override
    public int contentOriginY() {
        return 0;
    }

    @Override
    protected boolean diagramReady() {
        return layout != null;
    }

    @Override
    protected void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom) {
        for (FlowchartSubgraph subgraph : document.getSubgraphs()) {
            renderSubgraphBackground(context, subgraph, baseX, baseY, activeZoom, 0);
        }
        renderEdges(context, baseX, baseY, activeZoom);
        renderNodes(context, baseX, baseY, activeZoom);
        for (FlowchartSubgraph subgraph : document.getSubgraphs()) {
            renderSubgraphLabels(context, subgraph, baseX, baseY, activeZoom, 0);
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int previousContentOffsetX = getRawOffsetX();
        int previousContentOffsetY = getRawOffsetY();
        int previousViewportWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int previousViewportHeight = Math.max(1, bounds.height() - CANVAS_PADDING * 2);
        int previousContentWidth = layout != null ? layout.getWidth() : 0;
        int previousContentHeight = layout != null ? layout.getHeight() : 0;

        int safeWidth = preferredWidth > 0 ? Math.clamp(preferredWidth, 1, availableWidth)
            : Math.max(1, availableWidth);

        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(document.getLayoutMode());
        var minSizes = computeNodeMinSizes(context, safeWidth);
        int labelHeight = context.getLineHeight(NODE_TEXT_STYLE);
        layout = strategy.layout(
            document,
            minSizes,
            label -> new LabelSize(MermaidNodeRenderer.measureText(context, NODE_TEXT_STYLE, label), labelHeight));

        int desiredHeight = (layout != null ? layout.getHeight() : 0) + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.clamp(desiredHeight, MIN_HEIGHT, MAX_HEIGHT);
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);
        int innerViewportHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);

        restoreViewportAfterLayout(
            previousContentOffsetX,
            previousContentOffsetY,
            previousViewportWidth,
            previousViewportHeight,
            previousContentWidth,
            previousContentHeight,
            viewportWidth,
            innerViewportHeight);

        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    private Map<String, NodeMinSize> computeNodeMinSizes(LayoutContext context, int availableWidth) {
        int innerWidth = Math.max(1, availableWidth - CANVAS_PADDING * 2);
        int maxTextWidth = Math.clamp(innerWidth / 3, 72, 180);
        Map<String, NodeMinSize> result = new LinkedHashMap<>();
        nodeContentLayouts.clear();
        nodeContentSizes.clear();
        nodeLabelLayouts.clear();
        for (var entry : document.getNodes()
            .entrySet()) {
            String nodeId = entry.getKey();
            FlowchartNode node = entry.getValue();
            boolean isRoot = isRootNode(nodeId);

            LytBlock block = nodeContentBlocks.get(nodeId);
            int textWidth;
            int textHeight;

            if (block != null) {
                LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
                int contentWidth = Math.clamp(maxTextWidth + 60, 96, 240);
                block.layout(localContext, 0, 0, contentWidth);
                LytRect vb = resolveBlockVisualBounds(block);
                textWidth = vb.width();
                textHeight = vb.height();
                nodeContentLayouts.put(nodeId, new NodeContentLayout(block, vb));
            } else {
                ResolvedTextStyle style = isRoot ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
                String label = node.getLabel();
                List<String> lines = MermaidNodeRenderer
                    .wrapText(context, style, label != null ? label : "", maxTextWidth);
                if (lines.isEmpty()) lines = List.of(" ");
                textWidth = 0;
                for (String line : lines) {
                    textWidth = Math.max(textWidth, MermaidNodeRenderer.measureText(context, style, line));
                }
                int lineHeight = context.getLineHeight(style);
                // Keep wrapping in diagram coordinates; zoom must not change the line count.
                nodeLabelLayouts.put(nodeId, new NodeLabelLayout(lines, lineHeight));
                textHeight = Math.max(1, lines.size()) * lineHeight;
            }

            int contentW = textWidth;
            int contentH = textHeight;

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = MermaidNodeRenderer.measureText(context, ICON_TEXT_STYLE, badgeText)
                        + BADGE_PADDING_X * 2;
                    int badgeHeight = context.getLineHeight(ICON_TEXT_STYLE) + BADGE_PADDING_Y * 2;
                    contentW = Math.max(contentW, badgeWidth);
                    contentH += badgeHeight + ICON_GAP_Y;
                }
            }

            nodeContentSizes.put(nodeId, new NodeMinSize(contentW, contentH));
            LytRect minRect = FlowchartShapes.minNodeRect(
                node.getShape(),
                contentW,
                contentH,
                isRoot ? 15 : NODE_PADDING_X,
                isRoot ? 9 : NODE_PADDING_Y);
            int width = minRect.width();
            int height = minRect.height();

            if (FlowchartShapes.isShapeClipped(node.getShape())) {
                width += NODE_EDGE_MARGIN * 2;
                height += NODE_EDGE_MARGIN * 2;
            }

            result.put(nodeId, new NodeMinSize(width, height));
        }
        return result;
    }

    private void restoreViewportAfterLayout(int previousOffsetX, int previousOffsetY, int previousViewportWidth,
        int previousViewportHeight, int previousContentWidth, int previousContentHeight, int viewportWidth,
        int viewportHeight) {
        if (previousContentWidth <= 0 || previousContentHeight <= 0) {
            centerDiagram(viewportWidth, viewportHeight, layout.getWidth(), layout.getHeight());
            return;
        }
        float curZoom = getRawZoom();
        float anchorX = (previousViewportWidth * 0.5f - previousOffsetX) / Math.max(curZoom, 0.0001f);
        float anchorY = (previousViewportHeight * 0.5f - previousOffsetY) / Math.max(curZoom, 0.0001f);
        setContentOffset(
            Math.round(viewportWidth * 0.5f - anchorX * curZoom),
            Math.round(viewportHeight * 0.5f - anchorY * curZoom));
        clampOffsets();
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    private void renderEdges(RenderContext context, int baseX, int baseY, float activeZoom) {
        int defaultColor = context.resolveColor(EDGE_COLOR);
        for (EdgePath edgePath : layout.getEdgePaths()) {
            FlowchartEdge flowEdge = lookupEdge(edgePath.getFromId(), edgePath.getToId(), edgePath.getEdgeId());
            MermaidEdgeStyle style = flowEdge != null ? flowEdge.getStyle() : MermaidEdgeStyle.SOLID;
            boolean arrowFwd = flowEdge == null || flowEdge.isArrowFwd();
            boolean arrowRev = flowEdge != null && flowEdge.isArrowRev();
            MermaidArrowHead fwdHead = flowEdge != null ? flowEdge.getForwardHead() : MermaidArrowHead.TRIANGLE;
            MermaidArrowHead revHead = flowEdge != null ? flowEdge.getReverseHead() : MermaidArrowHead.NONE;

            if (style == MermaidEdgeStyle.INVISIBLE) continue;

            int edgeColor = defaultColor;
            int edgeThickness = style == MermaidEdgeStyle.THICK ? CONNECTOR_THICKNESS * 2 : CONNECTOR_THICKNESS;
            if (flowEdge != null) {
                String edgeStyles = flowEdge.getStyleOverride();
                if (edgeStyles != null) {
                    String stroke = getStyleProperty(edgeStyles, "stroke");
                    if (stroke != null) {
                        int parsed = parseHexColor(stroke);
                        if (parsed != 0) edgeColor = parsed;
                    }
                    String width = getStyleProperty(edgeStyles, "stroke-width");
                    if (width != null) {
                        try {
                            edgeThickness = Math.max(
                                1,
                                Integer.parseInt(
                                    width.replace("px", "")
                                        .trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            List<FlowchartLayoutResult.Point> points = edgePath.getPoints();
            if (points.size() < 2) continue;

            FlowchartLayoutResult.Point srcBoundary = null;
            FlowchartLayoutResult.Point tgtBoundary = null;
            NodePosition srcPos = null;
            NodePosition tgtPos = null;

            if (arrowFwd) {
                tgtPos = layout.getPosition(edgePath.getToId());
                if (tgtPos != null) {
                    FlowchartNode tgtNode = document.getNodes()
                        .get(edgePath.getToId());
                    MermaidNodeShape tgtShape = tgtNode != null ? tgtNode.getShape() : null;
                    if (tgtShape != null && FlowchartShapes.isShapeClipped(tgtShape)) {
                        FlowchartLayoutResult.Point lastPoint = points.get(points.size() - 1);
                        LytRect tgtRect = new LytRect(
                            tgtPos.getX() + NODE_EDGE_MARGIN,
                            tgtPos.getY() + NODE_EDGE_MARGIN,
                            tgtPos.getWidth() - NODE_EDGE_MARGIN * 2,
                            tgtPos.getHeight() - NODE_EDGE_MARGIN * 2);
                        tgtBoundary = FlowchartShapes
                            .edgeIntersect(tgtRect, tgtShape, lastPoint.getX(), lastPoint.getY());
                    }
                }
            }
            if (arrowRev) {
                srcPos = layout.getPosition(edgePath.getFromId());
                if (srcPos != null) {
                    FlowchartNode srcNode = document.getNodes()
                        .get(edgePath.getFromId());
                    MermaidNodeShape srcShape = srcNode != null ? srcNode.getShape() : null;
                    if (srcShape != null && FlowchartShapes.isShapeClipped(srcShape)) {
                        FlowchartLayoutResult.Point firstPoint = points.get(0);
                        LytRect srcRect = new LytRect(
                            srcPos.getX() + NODE_EDGE_MARGIN,
                            srcPos.getY() + NODE_EDGE_MARGIN,
                            srcPos.getWidth() - NODE_EDGE_MARGIN * 2,
                            srcPos.getHeight() - NODE_EDGE_MARGIN * 2);
                        srcBoundary = FlowchartShapes
                            .edgeIntersect(srcRect, srcShape, firstPoint.getX(), firstPoint.getY());
                    }
                }
            }

            List<FlowchartLayoutResult.Point> edgePoints = new ArrayList<>(points);
            if (tgtBoundary != null) {
                FlowchartLayoutResult.Point lastOrig = points.get(points.size() - 1);
                if (tgtBoundary.getX() != lastOrig.getX() || tgtBoundary.getY() != lastOrig.getY()) {
                    edgePoints.add(tgtBoundary);
                }
            }
            if (srcBoundary != null) {
                FlowchartLayoutResult.Point firstOrig = points.get(0);
                if (srcBoundary.getX() != firstOrig.getX() || srcBoundary.getY() != firstOrig.getY()) {
                    edgePoints.add(0, srcBoundary);
                }
            }

            drawEdgeLines(
                context,
                edgePoints,
                edgePath.getFromId(),
                edgePath.getToId(),
                baseX,
                baseY,
                activeZoom,
                style,
                edgeThickness,
                edgeColor,
                srcBoundary,
                tgtBoundary);

            if (arrowFwd) {
                float tipX, tipY, dirX, dirY;
                if (tgtPos != null) {
                    FlowchartNode tgtNode = document.getNodes()
                        .get(edgePath.getToId());
                    MermaidNodeShape tgtShape = tgtNode != null ? tgtNode.getShape() : null;
                    if (tgtBoundary != null) {
                        FlowchartLayoutResult.Point prev = edgePoints.get(edgePoints.size() - 2);
                        tipX = scaled(baseX, tgtBoundary.getX(), activeZoom);
                        tipY = scaled(baseY, tgtBoundary.getY(), activeZoom);
                        dirX = tipX - scaled(baseX, prev.getX(), activeZoom);
                        dirY = tipY - scaled(baseY, prev.getY(), activeZoom);
                    } else {
                        FlowchartLayoutResult.Point lastPoint = points.get(points.size() - 1);
                        FlowchartLayoutResult.Point prev = points.get(points.size() - 2);
                        int tx = tgtShape != null ? lastPoint.getX() : tgtPos.getX() + tgtPos.getWidth() / 2;
                        int ty = tgtShape != null ? lastPoint.getY() : tgtPos.getY() + tgtPos.getHeight() / 2;
                        tipX = scaled(baseX, tx, activeZoom);
                        tipY = scaled(baseY, ty, activeZoom);
                        dirX = tipX - scaled(baseX, prev.getX(), activeZoom);
                        dirY = tipY - scaled(baseY, prev.getY(), activeZoom);
                    }
                } else {
                    FlowchartLayoutResult.Point prev = points.get(points.size() - 2);
                    tipX = scaled(baseX, prev.getX(), activeZoom);
                    tipY = scaled(baseY, prev.getY(), activeZoom);
                    dirX = 0;
                    dirY = 0;
                }
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len > 0.5f) {
                    drawArrowHeadVariant(context, tipX, tipY, dirX / len, dirY / len, activeZoom, edgeColor, fwdHead);
                }
            }

            if (arrowRev) {
                float tailX, tailY, dirX, dirY;
                if (srcPos != null) {
                    FlowchartNode srcNode = document.getNodes()
                        .get(edgePath.getFromId());
                    MermaidNodeShape srcShape = srcNode != null ? srcNode.getShape() : null;
                    if (srcBoundary != null) {
                        FlowchartLayoutResult.Point next = edgePoints.get(1);
                        tailX = scaled(baseX, srcBoundary.getX(), activeZoom);
                        tailY = scaled(baseY, srcBoundary.getY(), activeZoom);
                        dirX = scaled(baseX, next.getX(), activeZoom) - tailX;
                        dirY = scaled(baseY, next.getY(), activeZoom) - tailY;
                    } else {
                        FlowchartLayoutResult.Point firstPoint = points.get(0);
                        FlowchartLayoutResult.Point second = points.get(1);
                        int tx = srcShape != null ? firstPoint.getX() : srcPos.getX() + srcPos.getWidth() / 2;
                        int ty = srcShape != null ? firstPoint.getY() : srcPos.getY() + srcPos.getHeight() / 2;
                        tailX = scaled(baseX, tx, activeZoom);
                        tailY = scaled(baseY, ty, activeZoom);
                        dirX = scaled(baseX, second.getX(), activeZoom) - tailX;
                        dirY = scaled(baseY, second.getY(), activeZoom) - tailY;
                    }
                } else {
                    FlowchartLayoutResult.Point second = points.get(1);
                    tailX = scaled(baseX, second.getX(), activeZoom);
                    tailY = scaled(baseY, second.getY(), activeZoom);
                    dirX = 0;
                    dirY = 0;
                }
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len > 0.5f) {
                    drawArrowHeadVariant(context, tailX, tailY, dirX / len, dirY / len, activeZoom, edgeColor, revHead);
                }
            }

        }
        for (var entry : layout.getEdgeLabelPositions()
            .entrySet()) {
            drawEdgeLabel(
                context,
                entry.getValue(),
                baseX,
                baseY,
                activeZoom,
                entry.getKey()
                    .getLabel());
        }
    }

    private @Nullable FlowchartEdge lookupEdge(String fromId, String toId, @Nullable String edgeId) {
        if (edgeId != null) {
            for (FlowchartEdge e : document.getEdges()) {
                if (edgeId.equals(e.getEdgeId())) return e;
            }
        }
        for (FlowchartEdge e : document.getEdges()) {
            if (e.getFrom()
                .equals(fromId)
                && e.getTo()
                    .equals(toId))
                return e;
        }
        return null;
    }

    private void drawEdgeLines(RenderContext context, List<FlowchartLayoutResult.Point> points, String fromId,
        String toId, int baseX, int baseY, float activeZoom, MermaidEdgeStyle style, int thickness, int color,
        @Nullable FlowchartLayoutResult.Point srcBoundary, @Nullable FlowchartLayoutResult.Point tgtBoundary) {
        NodePosition srcPos = layout.getPosition(fromId);
        NodePosition tgtPos = layout.getPosition(toId);

        float srcCx = srcPos != null ? scaled(baseX, srcPos.getX() + srcPos.getWidth() / 2, activeZoom) : Float.NaN;
        float srcCy = srcPos != null ? scaled(baseY, srcPos.getY() + srcPos.getHeight() / 2, activeZoom) : Float.NaN;
        float tgtCx = tgtPos != null ? scaled(baseX, tgtPos.getX() + tgtPos.getWidth() / 2, activeZoom) : Float.NaN;
        float tgtCy = tgtPos != null ? scaled(baseY, tgtPos.getY() + tgtPos.getHeight() / 2, activeZoom) : Float.NaN;

        int n = points.size();

        for (int i = 1; i < n; i++) {
            FlowchartLayoutResult.Point from = points.get(i - 1);
            FlowchartLayoutResult.Point to = points.get(i);
            float x1 = scaled(baseX, from.getX(), activeZoom);
            float y1 = scaled(baseY, from.getY(), activeZoom);
            float x2 = scaled(baseX, to.getX(), activeZoom);
            float y2 = scaled(baseY, to.getY(), activeZoom);
            drawLine(context, x1, y1, x2, y2, style, thickness, color);
        }

        if (!Float.isNaN(srcCx) && srcBoundary == null) {
            FlowchartLayoutResult.Point first = points.get(0);
            float x1 = scaled(baseX, first.getX(), activeZoom);
            float y1 = scaled(baseY, first.getY(), activeZoom);
            drawLine(context, srcCx, srcCy, x1, y1, style, thickness, color);
        }
        if (!Float.isNaN(tgtCx) && tgtBoundary == null) {
            FlowchartLayoutResult.Point last = points.get(n - 1);
            float x1 = scaled(baseX, last.getX(), activeZoom);
            float y1 = scaled(baseY, last.getY(), activeZoom);
            drawLine(context, x1, y1, tgtCx, tgtCy, style, thickness, color);
        }
    }

    private void drawLine(RenderContext context, float x1, float y1, float x2, float y2, MermaidEdgeStyle style,
        int thickness, int color) {
        if (style == MermaidEdgeStyle.DASHED || style == MermaidEdgeStyle.DOTTED) {
            drawDashedLine(context, x1, y1, x2, y2, thickness, color, style == MermaidEdgeStyle.DOTTED);
        } else {
            context.drawLine(x1, y1, x2, y2, thickness, color);
        }
    }

    private void drawDashedLine(RenderContext context, float x1, float y1, float x2, float y2, int thickness, int color,
        boolean dotted) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        float nx = dx / len;
        float ny = dy / len;
        float dashLen = dotted ? Math.max(1f, thickness * 0.5f) : Math.max(2f, thickness * 4f);
        float gapLen = dotted ? Math.max(2f, thickness * 1.5f) : Math.max(1f, thickness);
        float drawn = 0f;
        boolean draw = true;
        while (drawn < len) {
            float segEnd = Math.min(drawn + dashLen, len);
            float sx = x1 + nx * drawn;
            float sy = y1 + ny * drawn;
            float ex = x1 + nx * segEnd;
            float ey = y1 + ny * segEnd;
            if (draw) {
                context.drawLine(sx, sy, ex, ey, thickness, color);
            }
            drawn = segEnd + gapLen;
            draw = !draw;
        }
    }

    private void drawArrowHeadVariant(RenderContext context, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color, MermaidArrowHead headType) {
        switch (headType) {
            case CIRCLE -> drawCircleHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
            case CROSS -> drawCrossHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
            default -> drawTriangleHead(context, tipX, tipY, dirX, dirY, activeZoom, color);
        }
    }

    private void drawTriangleHead(RenderContext context, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color) {
        float size = Math.max(4f, 8f * activeZoom);
        float perpX = -dirY;
        float baseX = tipX - dirX * size;
        float baseY = tipY - dirY * size;
        float leftX = baseX + perpX * size * 0.4f;
        float leftY = baseY + dirX * size * 0.4f;
        float rightX = baseX - perpX * size * 0.4f;
        float rightY = baseY - dirX * size * 0.4f;
        context.fillTriangle(tipX, tipY, leftX, leftY, rightX, rightY, color);
    }

    private void drawCircleHead(RenderContext context, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float radius = Math.max(3f, 5f * activeZoom);
        float cx = tipX - dirX * radius;
        float cy = tipY - dirY * radius;
        context.fillCircle(cx, cy, radius, color);
    }

    private void drawCrossHead(RenderContext context, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float size = Math.max(3f, 5f * activeZoom);
        float perpX = -dirY;
        float cx = tipX - dirX * size * 0.5f;
        float cy = tipY - dirY * size * 0.5f;
        float thickness = Math.max(1f, 1.5f * activeZoom);
        context.drawLine(
            cx + perpX * size * 0.7f,
            cy + dirX * size * 0.7f,
            cx - perpX * size * 0.7f,
            cy - dirX * size * 0.7f,
            thickness,
            color);
        context.drawLine(
            cx + perpX * size * 0.7f,
            cy - dirX * size * 0.7f,
            cx - perpX * size * 0.7f,
            cy + dirX * size * 0.7f,
            thickness,
            color);
    }

    private void drawEdgeLabel(RenderContext context, NodePosition position, int baseX, int baseY, float activeZoom,
        String label) {
        int screenX = scaled(baseX, position.getCenterX(), activeZoom);
        int screenY = scaled(baseY, position.getCenterY(), activeZoom);
        ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
        int textWidth = context.getStringWidth(label, labelStyle);
        int textHeight = context.getLineHeight(labelStyle);
        int pad = Math.max(1, Math.round(2 * activeZoom));
        int bgColor = context.resolveColor(new ConstantColor(ColorUtils.ARGB_CC0C1117.getColor()));
        LytRect bg = new LytRect(
            screenX - textWidth / 2 - pad,
            screenY - textHeight / 2 - pad,
            textWidth + pad * 2,
            textHeight + pad * 2);
        context.fillRect(bg, bgColor);
        context.drawText(label, screenX - textWidth / 2, screenY - textHeight / 2, labelStyle);
    }

    private void renderNodes(RenderContext context, int baseX, int baseY, float activeZoom) {
        RenderContext shapeContext = new NodeContentRenderContext(
            context,
            getInnerViewport(),
            baseX,
            baseY,
            activeZoom);
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            NodePosition pos = entry.getValue();
            FlowchartNode node = document.getNodes()
                .get(nodeId);
            if (node == null) continue;

            boolean isRoot = isRootNode(nodeId);
            ResolvedTextStyle style = getOrScaleStyle(isRoot ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE, activeZoom);

            LytRect rect = nodeVisualRect(node, pos);

            var colors = MermaidNodeRenderer.resolveNodeColors(node.getClasses(), node.getShape(), isRoot);
            String nodeStyles = node.getStyleOverride();
            if (nodeStyles != null) {
                String fill = getStyleProperty(nodeStyles, "fill");
                String stroke = getStyleProperty(nodeStyles, "stroke");
                if (fill != null) {
                    int fillColor = parseHexColor(fill);
                    if (fillColor != 0)
                        colors = new MermaidNodeRenderer.NodeColors(fillColor, colors.border(), colors.accent());
                }
                if (stroke != null) {
                    int strokeColor = parseHexColor(stroke);
                    if (strokeColor != 0)
                        colors = new MermaidNodeRenderer.NodeColors(colors.background(), strokeColor, colors.accent());
                }
            }
            FlowchartShapes.render(shapeContext, rect, node.getShape(), colors.background(), colors.border());
            if (colors.accent() != MermaidNodeRenderer.DEFAULT_ACCENT
                && FlowchartShapes.hasAccentBar(node.getShape())) {
                MermaidNodeRenderer.renderAccentBar(shapeContext, rect, colors.accent());
            }

            LytRect contentArea = nodeContentArea(nodeId, pos, baseX, baseY, activeZoom);
            int textY = contentArea.y();

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = Math.max(
                        1,
                        context.getStringWidth(badgeText, badgeStyle)
                            + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)) * 2);
                    int badgeHeight = Math.max(
                        1,
                        context.getLineHeight(badgeStyle) + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)) * 2);
                    int badgeX = contentArea.x();
                    LytRect badge = new LytRect(badgeX, textY, badgeWidth, badgeHeight);
                    context.fillRect(badge, MermaidNodeRenderer.BADGE_BACKGROUND);
                    context.drawBorder(badge, MermaidNodeRenderer.BADGE_BORDER, 1);
                    context.drawText(
                        badgeText,
                        badge.x() + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)),
                        badge.y() + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)),
                        badgeStyle);
                    textY += badgeHeight + Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
                }
            }

            int visibleWidth = contentArea.width();
            int visibleHeight = contentArea.height();

            NodeContentLayout contentLayout = nodeContentLayouts.get(nodeId);
            if (contentLayout != null) {
                renderNodeContent(
                    context,
                    contentLayout,
                    new LytRect(contentArea.x(), textY, visibleWidth, contentArea.bottom() - textY),
                    activeZoom);
            } else {
                String label = node.getLabel();
                if (label == null || label.isEmpty()) continue;

                NodeLabelLayout labelLayout = nodeLabelLayouts.get(nodeId);
                List<String> lines = labelLayout.lines();
                int totalTextHeight = Math.round(lines.size() * labelLayout.lineHeight() * activeZoom);
                int textAreaHeight = contentArea.y() + visibleHeight - textY;
                int baseTextY = textY + Math.max(0, (textAreaHeight - totalTextHeight) / 2);
                for (int i = 0; i < lines.size(); i++) {
                    int lineWidth = context.getStringWidth(lines.get(i), style);
                    int textX = contentArea.x() + Math.max(0, (visibleWidth - lineWidth) / 2);
                    context.drawText(
                        lines.get(i),
                        textX,
                        baseTextY + Math.round(i * labelLayout.lineHeight() * activeZoom),
                        style);
                }
            }
        }
    }

    private record NodeLabelLayout(List<String> lines, int lineHeight) {}

    private boolean isRootNode(String nodeId) {
        return !document.getNodeOrder()
            .isEmpty() && nodeId.equals(
                document.getNodeOrder()
                    .get(0));
    }

    private static LytRect nodeVisualRect(FlowchartNode node, NodePosition position) {
        int margin = FlowchartShapes.isShapeClipped(node.getShape()) ? NODE_EDGE_MARGIN : 0;
        return new LytRect(
            position.getX() + margin,
            position.getY() + margin,
            position.getWidth() - margin * 2,
            position.getHeight() - margin * 2);
    }

    private LytRect nodeContentArea(String nodeId, NodePosition position, int baseX, int baseY, float zoom) {
        FlowchartNode node = document.getNodes()
            .get(nodeId);
        LytRect visualRect = nodeVisualRect(node, position);
        NodeMinSize size = nodeContentSizes.get(nodeId);
        boolean isRoot = isRootNode(nodeId);
        LytRect area = FlowchartShapes.contentBounds(
            visualRect,
            node.getShape(),
            size.width(),
            size.height(),
            isRoot ? 15 : NODE_PADDING_X,
            isRoot ? 9 : NODE_PADDING_Y);
        return new LytRect(
            scaled(baseX, area.x(), zoom),
            scaled(baseY, area.y(), zoom),
            Math.max(1, Math.round(area.width() * zoom)),
            Math.max(1, Math.round(area.height() * zoom)));
    }

    private void renderNodeContent(RenderContext context, NodeContentLayout contentLayout, LytRect contentArea,
        float activeZoom) {
        LytRect contentViewport = nodeContentViewport(contentLayout, contentArea, activeZoom);
        renderNodeContent(context, contentLayout.block(), contentViewport, contentLayout.visualBounds(), activeZoom);
    }

    private static LytRect nodeContentViewport(NodeContentLayout contentLayout, LytRect contentArea, float activeZoom) {
        LytRect rawViewport = new LytRect(
            contentArea.x(),
            contentArea.y(),
            Math.max(
                1,
                Math.round(
                    contentLayout.visualBounds()
                        .width() * activeZoom)),
            Math.max(
                1,
                Math.round(
                    contentLayout.visualBounds()
                        .height() * activeZoom)));

        int cvpX = rawViewport.x();
        int cvpY = rawViewport.y();
        if (rawViewport.width() < contentArea.width()) {
            cvpX = contentArea.x() + (contentArea.width() - rawViewport.width()) / 2;
        }
        if (rawViewport.height() < contentArea.height()) {
            cvpY = contentArea.y() + (contentArea.height() - rawViewport.height()) / 2;
        }
        return new LytRect(cvpX, cvpY, rawViewport.width(), rawViewport.height());
    }

    @Override
    @Nullable
    protected NodeHit pickNodeHit(int documentX, int documentY) {
        if (layout == null) return null;
        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            NodeContentLayout contentLayout = nodeContentLayouts.get(nodeId);
            if (contentLayout == null) continue;

            NodePosition pos = entry.getValue();
            LytRect area = nodeContentArea(nodeId, pos, baseX, baseY, activeZoom);
            int badgeHeight = resolveNodeBadgeHeight(nodeId, activeZoom);
            LytRect contentScreenRect = nodeContentViewport(
                contentLayout,
                new LytRect(area.x(), area.y() + badgeHeight, area.width(), area.height() - badgeHeight),
                activeZoom);

            if (!contentScreenRect.contains(documentX, documentY)) continue;

            int localX = unscaleCoordinate(documentX - contentScreenRect.x(), activeZoom) + contentLayout.visualBounds()
                .x();
            int localY = unscaleCoordinate(documentY - contentScreenRect.y(), activeZoom) + contentLayout.visualBounds()
                .y();
            DocumentInteractionSnapshot hit = LytDocument.pick(contentLayout.block(), localX, localY);
            if (hit != null) {
                return new NodeHit(hit.node(), hit.flowPath(), localX, localY);
            }
        }
        return null;
    }

    private int resolveNodeBadgeHeight(String nodeId, float activeZoom) {
        FlowchartNode node = document.getNodes()
            .get(nodeId);
        if (node == null || node.getIcon() == null) return 0;
        String badgeText = MermaidNodeRenderer.simplifyIcon(node.getIcon());
        if (badgeText == null) return 0;
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        return contextLineHeight(badgeStyle) + badgePaddingY * 2 + iconGapY;
    }

    private void renderSubgraphBackground(RenderContext context, FlowchartSubgraph subgraph, int baseX, int baseY,
        float activeZoom, int depth) {
        NodePosition position = layout.getSubgraphPositions()
            .get(subgraph.getId());
        if (position == null) return;

        int sx = scaled(baseX, position.getX(), activeZoom);
        int sy = scaled(baseY, position.getY(), activeZoom);
        int sw = Math.max(1, Math.round(position.getWidth() * activeZoom));
        int sh = Math.max(1, Math.round(position.getHeight() * activeZoom));
        LytRect sgRect = new LytRect(sx, sy, sw, sh);

        int bg = context.resolveColor(SUBGRAPH_BG[depth % SUBGRAPH_BG.length]);
        int border = context.resolveColor(SUBGRAPH_BORDER[depth % SUBGRAPH_BORDER.length]);
        context.fillRect(sgRect, bg);
        context.drawBorder(sgRect, border, Math.max(1, Math.round(1.5f * activeZoom)));

        for (var child : subgraph.getChildren()) {
            renderSubgraphBackground(context, child, baseX, baseY, activeZoom, depth + 1);
        }
    }

    private void renderSubgraphLabels(RenderContext context, FlowchartSubgraph subgraph, int baseX, int baseY,
        float activeZoom, int depth) {
        String label = subgraph.getLabel();
        NodePosition labelPosition = layout.getSubgraphLabelPositions()
            .get(subgraph.getId());
        if (labelPosition != null) {
            ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
            int labelX = scaled(baseX, labelPosition.getX(), activeZoom);
            int labelY = scaled(baseY, labelPosition.getY(), activeZoom);
            int padding = Math.max(1, Math.round(2 * activeZoom));
            context.fillRect(
                new LytRect(
                    labelX - padding,
                    labelY - padding,
                    context.getStringWidth(label, labelStyle) + padding * 2,
                    context.getLineHeight(labelStyle) + padding * 2),
                context.resolveColor(SUBGRAPH_BG[depth % SUBGRAPH_BG.length]) | 0xFF000000);
            context.drawText(label, labelX, labelY, labelStyle);
        }

        for (var child : subgraph.getChildren()) {
            renderSubgraphLabels(context, child, baseX, baseY, activeZoom, depth + 1);
        }
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

    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            if (h.length() == 6) {
                return ColorUtils.BLACK.getColor() | Integer.parseInt(h, 16);
            } else if (h.length() == 8) {
                return (int) Long.parseLong(h, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    @Override
    public List<ComponentEntry> getDebugComponents() {
        List<ComponentEntry> cachedComponents = getCachedDebugComponents(layout);
        if (cachedComponents != null) {
            return cachedComponents;
        }
        List<ComponentEntry> components = new ArrayList<>();
        if (layout == null || bounds == null) {
            return components;
        }
        LytRect viewport = getInnerViewport();
        float zoom = getActiveZoom();
        int baseX = viewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = viewport.y() + getVisualOffsetY() - getScaledOriginY();

        for (EdgePath edge : layout.getEdgePaths()) {
            List<FlowchartLayoutResult.Point> points = edge.getPoints();
            for (int index = 1; index < points.size(); index++) {
                var from = points.get(index - 1);
                var to = points.get(index);
                components.add(
                    new LineComponentEntry(
                        "Edge:" + edge.getFromId() + "->" + edge.getToId(),
                        scaled(baseX, from.getX(), zoom),
                        scaled(baseY, from.getY(), zoom),
                        scaled(baseX, to.getX(), zoom),
                        scaled(baseY, to.getY(), zoom),
                        Math.max(3, Math.round(CONNECTOR_THICKNESS * zoom) + 2),
                        null,
                        10));
            }
        }

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            NodePosition position = entry.getValue();
            int x = scaled(baseX, position.getX(), zoom);
            int y = scaled(baseY, position.getY(), zoom);
            int width = Math.max(1, Math.round(position.getWidth() * zoom));
            int height = Math.max(1, Math.round(position.getHeight() * zoom));
            LytRect nodeBounds = new LytRect(x, y, width, height);
            FlowchartNode node = document.getNodes()
                .get(entry.getKey());
            String label = node != null && node.getLabel() != null ? node.getLabel() : entry.getKey();
            components.add(new SimpleComponentEntry("Node:" + label, nodeBounds, null, 20));
            LytRect contentBounds = nodeContentArea(entry.getKey(), position, baseX, baseY, zoom);
            components.add(new SimpleComponentEntry("Label:" + label, contentBounds, null, 25));
            NodeContentLayout contentLayout = nodeContentLayouts.get(entry.getKey());
            if (contentLayout != null) {
                int badgeHeight = resolveNodeBadgeHeight(entry.getKey(), zoom);
                LytRect viewportBounds = nodeContentViewport(
                    contentLayout,
                    new LytRect(
                        contentBounds.x(),
                        contentBounds.y() + badgeHeight,
                        contentBounds.width(),
                        contentBounds.height() - badgeHeight),
                    zoom);
                collectNodeContentDebugComponents(contentLayout, viewportBounds, zoom, label, 30, components);
            }
        }
        return cacheDebugComponents(layout, components);
    }
}
