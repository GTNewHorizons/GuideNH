package com.hfstudio.guidenh.guide.document.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.shapes.FlowchartShapes;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartEdge;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.EdgePath;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodeMinSize;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodePosition;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartSubgraph;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class LytMermaidFlowchartCanvas extends LytMermaidCanvas<LytMermaidFlowchartCanvas> {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 120;
    private static final int MAX_HEIGHT = 320;
    private static final int CONNECTOR_THICKNESS = 2;
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int ICON_GAP_Y = 4;
    private static final int BADGE_PADDING_X = 4;
    private static final int BADGE_PADDING_Y = 2;
    private static final ConstantColor NODE_TEXT = new ConstantColor(0xFFD7DEE7);
    private static final ConstantColor ROOT_TEXT_COLOR = new ConstantColor(0xFFF1F6FB);
    private static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(0xFFB8C2CF);
    private static final ConstantColor EDGE_COLOR = new ConstantColor(0xFF5D6C7C);
    private static final ConstantColor[] SUBGRAPH_BG = { new ConstantColor(0x301E2A45), new ConstantColor(0x302A1E45),
        new ConstantColor(0x301E2A2A), new ConstantColor(0x302A2A1E), };
    private static final ConstantColor[] SUBGRAPH_BORDER = { new ConstantColor(0x99434C57),
        new ConstantColor(0x994C5743), new ConstantColor(0x99575743), new ConstantColor(0x9943574C), };
    private static final int SUBGRAPH_PADDING = 8;
    private static final int SUBGRAPH_LABEL_HEIGHT = 14;

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
    public boolean usePrimitives() {
        return true;
    }

    @Override
    protected void emitDiagramPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {
        emitSubgraphsPrimitives(c, baseX, baseY, activeZoom);
        emitEdgesPrimitives(c, baseX, baseY, activeZoom);
        emitNodesPrimitives(c, baseX, baseY, activeZoom);
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
        var minSizes = computeNodeMinSizes(context);
        layout = strategy.layout(document, minSizes);

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

    private Map<String, NodeMinSize> computeNodeMinSizes(LayoutContext context) {
        int innerWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int maxTextWidth = Math.clamp(innerWidth / 3, 72, 180);
        String rootNodeId = document.getNodeOrder()
            .isEmpty() ? null
                : document.getNodeOrder()
                    .get(0);

        Map<String, NodeMinSize> result = new LinkedHashMap<>();
        nodeContentLayouts.clear();
        for (var entry : document.getNodes()
            .entrySet()) {
            String nodeId = entry.getKey();
            FlowchartNode node = entry.getValue();
            boolean isRoot = nodeId.equals(rootNodeId);

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

            LytRect minRect = FlowchartShapes
                .minNodeRect(node.getShape(), contentW, contentH, NODE_PADDING_X, NODE_PADDING_Y);
            int width = minRect.width();
            int height = minRect.height();

            if (isRoot) {
                width += 10;
                height += 4;
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
    protected void afterExternalLayout() {
        // The Java layout pre-pass no longer calls computeLayout, so the
        // diagram layout would never be computed. Compute it here from the
        // Rust-computed bounds, using a GuideText-backed LayoutContext.
        if (layout != null) return; // already computed (direct layout() call)
        int safeWidth = preferredWidth > 0
            ? Math.clamp(preferredWidth, 1, Math.max(1, bounds.width()))
            : Math.max(1, bounds.width());
        LayoutContext fallbackCtx = new LayoutContext(new FontMetrics() {
            @Override
            public float getAdvance(int codePoint, com.hfstudio.guidenh.guide.style.ResolvedTextStyle s) {
                return GuideText.measureWidth(new String(Character.toChars(codePoint)), s);
            }
            @Override
            public int getLineHeight(com.hfstudio.guidenh.guide.style.ResolvedTextStyle s) {
                return GuideText.lineHeight(s);
            }
        });
        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(document.getLayoutMode());
        var minSizes = computeNodeMinSizes(fallbackCtx);
        layout = strategy.layout(document, minSizes);

        // After computing diagram layout, derive the desired viewport height,
        // write it back to bounds, set preferredHeight for the next Rust pass,
        // and trigger a re-layout so the corrected height takes effect.
        if (layout != null) {
            int desiredHeight = layout.getHeight() + CANVAS_PADDING * 2;
            int newHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
                : Math.clamp(desiredHeight, MIN_HEIGHT, MAX_HEIGHT);
            if (newHeight != bounds.height()) {
                preferredHeight = newHeight;
                bounds = new LytRect(bounds.x(), bounds.y(), bounds.width(), newHeight);
                LytDocument doc = getDocument();
                if (doc != null) {
                    doc.invalidateLayout();
                }
            }
        }
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

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
            int sx = scaled(baseX, pos.getX(), activeZoom);
            int sy = scaled(baseY, pos.getY(), activeZoom);
            int sw = Math.max(1, Math.round(pos.getWidth() * activeZoom));
            int sh = Math.max(1, Math.round(pos.getHeight() * activeZoom));
            LytRect nodeRect = new LytRect(sx, sy, sw, sh);

            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int contentY = nodeRect.y() + Math.max(1, Math.round(NODE_PADDING_Y * activeZoom))
                + resolveNodeBadgeHeight(entry.getKey(), activeZoom);
            LytRect contentScreenRect = resolveNodeContentRect(contentLayout, nodeRect, paddingX, contentY, activeZoom);

            if (!contentScreenRect.contains(documentX, documentY)) continue;

            int localX = unscaleCoordinate(documentX - contentScreenRect.x(), activeZoom);
            int localY = unscaleCoordinate(documentY - contentScreenRect.y(), activeZoom);
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

    // ---- primitives pipeline (replaces render* for the primitives path) ----

    private void emitSubgraphsPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {
        if (layout == null) return;
        for (var subgraph : document.getSubgraphs()) {
            emitSubgraphRecursive(c, subgraph, layout.getNodePositions(), baseX, baseY, activeZoom, 0);
        }
    }

    private void emitSubgraphRecursive(PrimitiveCollector c, FlowchartSubgraph subgraph,
        Map<String, NodePosition> positions, int baseX, int baseY, float activeZoom, int depth) {
        LytRect bounds = computeSubgraphBounds(subgraph, positions);
        if (bounds == null) return;

        int pad = Math.round(SUBGRAPH_PADDING * activeZoom);
        int sx = scaled(baseX, bounds.x() - pad, activeZoom);
        int sy = scaled(baseY, bounds.y() - pad, activeZoom);
        int sw = Math.max(1, Math.round((bounds.width() + pad * 2) * activeZoom));
        int sh = Math.max(1, Math.round((bounds.height() + pad * 2) * activeZoom));
        LytRect sgRect = new LytRect(sx, sy, sw, sh);

        int bg = SUBGRAPH_BG[depth % SUBGRAPH_BG.length].resolve(LightDarkMode.current());
        int border = SUBGRAPH_BORDER[depth % SUBGRAPH_BORDER.length].resolve(LightDarkMode.current());
        c.emit(new GuideRenderPrimitive.FillRect(sgRect.x(), sgRect.y(), sgRect.width(), sgRect.height(), bg));
        int borderThickness = Math.max(1, Math.round(1.5f * activeZoom));
        c.emit(new GuideRenderPrimitive.DrawBorder(
            sgRect.x(), sgRect.y(), sgRect.width(), sgRect.height(),
            borderThickness, borderThickness, borderThickness, borderThickness, border));

        String label = subgraph.getLabel();
        if (label != null && !label.isEmpty()) {
            int labelPadX = Math.max(2, Math.round(4 * activeZoom));
            int labelPadY = Math.max(1, Math.round(2 * activeZoom));
            ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
            c.emit(new GuideRenderPrimitive.DrawText(
                label,
                sgRect.x() + labelPadX,
                sgRect.y() + labelPadY,
                labelStyle));
        }

        for (var child : subgraph.getChildren()) {
            emitSubgraphRecursive(c, child, positions, baseX, baseY, activeZoom, depth + 1);
        }
    }

    private void emitEdgesPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {
        int defaultColor = EDGE_COLOR.resolve(LightDarkMode.current());
        for (EdgePath edgePath : layout.getEdgePaths()) {
            FlowchartEdge flowEdge = lookupEdge(edgePath.getFromId(), edgePath.getToId(), edgePath.getEdgeId());
            MermaidEdgeStyle style = flowEdge != null ? flowEdge.getStyle() : MermaidEdgeStyle.SOLID;
            boolean arrowFwd = flowEdge == null || flowEdge.isArrowFwd();
            boolean arrowRev = flowEdge != null && flowEdge.isArrowRev();
            MermaidArrowHead fwdHead = flowEdge != null ? flowEdge.getForwardHead() : MermaidArrowHead.TRIANGLE;
            MermaidArrowHead revHead = flowEdge != null ? flowEdge.getReverseHead() : MermaidArrowHead.NONE;
            String label = flowEdge != null ? flowEdge.getLabel() : null;

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

            for (int i = 1; i < points.size(); i++) {
                FlowchartLayoutResult.Point from = points.get(i - 1);
                FlowchartLayoutResult.Point to = points.get(i);
                float x1 = scaled(baseX, from.getX(), activeZoom);
                float y1 = scaled(baseY, from.getY(), activeZoom);
                float x2 = scaled(baseX, to.getX(), activeZoom);
                float y2 = scaled(baseY, to.getY(), activeZoom);

                if (style == MermaidEdgeStyle.DASHED || style == MermaidEdgeStyle.DOTTED) {
                    emitDashedLine(c, x1, y1, x2, y2, edgeThickness, edgeColor, style == MermaidEdgeStyle.DOTTED);
                } else {
                    c.emit(new GuideRenderPrimitive.DrawLine(x1, y1, x2, y2, edgeThickness, edgeColor));
                }
            }

            if (arrowFwd || arrowRev) {
                FlowchartLayoutResult.Point last = points.getLast();
                FlowchartLayoutResult.Point prev = points.size() >= 2 ? points.get(points.size() - 2) : last;
                float tipX = scaled(baseX, last.getX(), activeZoom);
                float tipY = scaled(baseY, last.getY(), activeZoom);
                float dirX = tipX - scaled(baseX, prev.getX(), activeZoom);
                float dirY = tipY - scaled(baseY, prev.getY(), activeZoom);
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len > 0.5f) {
                    dirX /= len;
                    dirY /= len;
                    if (arrowFwd) {
                        emitArrowHeadVariant(c, tipX, tipY, dirX, dirY, activeZoom, edgeColor, fwdHead);
                    }
                }

                if (arrowRev) {
                    FlowchartLayoutResult.Point first = points.get(0);
                    FlowchartLayoutResult.Point second = points.size() >= 2 ? points.get(1) : first;
                    float tailX = scaled(baseX, first.getX(), activeZoom);
                    float tailY = scaled(baseY, first.getY(), activeZoom);
                    float revDirX = tailX - scaled(baseX, second.getX(), activeZoom);
                    float revDirY = tailY - scaled(baseY, second.getY(), activeZoom);
                    float revLen = (float) Math.sqrt(revDirX * revDirX + revDirY * revDirY);
                    if (revLen > 0.5f) {
                        revDirX /= revLen;
                        revDirY /= revLen;
                        emitArrowHeadVariant(c, tailX, tailY, revDirX, revDirY, activeZoom, edgeColor, revHead);
                    }
                }
            }

            if (label != null && !label.isEmpty()) {
                emitEdgeLabelPrimitives(c, points, baseX, baseY, activeZoom, label);
            }
        }
    }

    private void emitDashedLine(PrimitiveCollector c, float x1, float y1, float x2, float y2, int thickness, int color,
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
                c.emit(new GuideRenderPrimitive.DrawLine(sx, sy, ex, ey, thickness, color));
            }
            drawn = segEnd + gapLen;
            draw = !draw;
        }
    }

    private void emitArrowHeadVariant(PrimitiveCollector c, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color, MermaidArrowHead headType) {
        switch (headType) {
            case CIRCLE -> emitCircleHead(c, tipX, tipY, dirX, dirY, activeZoom, color);
            case CROSS -> emitCrossHead(c, tipX, tipY, dirX, dirY, activeZoom, color);
            default -> emitTriangleHead(c, tipX, tipY, dirX, dirY, activeZoom, color);
        }
    }

    private void emitTriangleHead(PrimitiveCollector c, float tipX, float tipY, float dirX, float dirY,
        float activeZoom, int color) {
        float size = Math.max(4f, 8f * activeZoom);
        float perpX = -dirY;
        float baseX = tipX - dirX * size;
        float baseY = tipY - dirY * size;
        float leftX = baseX + perpX * size * 0.4f;
        float leftY = baseY + dirX * size * 0.4f;
        float rightX = baseX - perpX * size * 0.4f;
        float rightY = baseY - dirX * size * 0.4f;
        c.emit(new GuideRenderPrimitive.DrawTriangle(tipX, tipY, leftX, leftY, rightX, rightY, color));
    }

    private void emitCircleHead(PrimitiveCollector c, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float radius = Math.max(3f, 5f * activeZoom);
        float cx = tipX - dirX * radius;
        float cy = tipY - dirY * radius;
        c.emit(new GuideRenderPrimitive.DrawCircle(cx, cy, radius, color, true));
    }

    private void emitCrossHead(PrimitiveCollector c, float tipX, float tipY, float dirX, float dirY, float activeZoom,
        int color) {
        float size = Math.max(3f, 5f * activeZoom);
        float perpX = -dirY;
        float cx = tipX - dirX * size * 0.5f;
        float cy = tipY - dirY * size * 0.5f;
        float thickness = Math.max(1f, 1.5f * activeZoom);
        c.emit(new GuideRenderPrimitive.DrawLine(
            cx + perpX * size * 0.7f, cy + dirX * size * 0.7f,
            cx - perpX * size * 0.7f, cy - dirX * size * 0.7f,
            thickness, color));
        c.emit(new GuideRenderPrimitive.DrawLine(
            cx + perpX * size * 0.7f, cy - dirX * size * 0.7f,
            cx - perpX * size * 0.7f, cy + dirX * size * 0.7f,
            thickness, color));
    }

    private void emitEdgeLabelPrimitives(PrimitiveCollector c, List<FlowchartLayoutResult.Point> points, int baseX,
        int baseY, float activeZoom, String label) {
        float totalLen = 0f;
        float[] segLens = new float[points.size() - 1];
        for (int i = 1; i < points.size(); i++) {
            float dx = points.get(i).getX() - points.get(i - 1).getX();
            float dy = points.get(i).getY() - points.get(i - 1).getY();
            segLens[i - 1] = (float) Math.sqrt(dx * dx + dy * dy);
            totalLen += segLens[i - 1];
        }
        if (totalLen < 1f) return;
        float halfLen = totalLen * 0.5f;
        float accumulated = 0f;
        float mx = points.getFirst().getX();
        float my = points.getFirst().getY();
        for (int i = 0; i < segLens.length; i++) {
            if (accumulated + segLens[i] >= halfLen) {
                float frac = (halfLen - accumulated) / Math.max(segLens[i], 0.0001f);
                mx = points.get(i).getX()
                    + (points.get(i + 1).getX() - points.get(i).getX()) * frac;
                my = points.get(i).getY()
                    + (points.get(i + 1).getY() - points.get(i).getY()) * frac;
                break;
            }
            accumulated += segLens[i];
        }
        int screenX = Math.round(scaled(baseX, Math.round(mx), activeZoom));
        int screenY = Math.round(scaled(baseY, Math.round(my), activeZoom));
        ResolvedTextStyle labelStyle = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
        int textWidth = GuideText.measureWidth(label, labelStyle);
        int textHeight = GuideText.lineHeight(labelStyle);
        int pad = Math.max(1, Math.round(2 * activeZoom));
        int bgColor = new ConstantColor(0xCC0C1117).resolve(LightDarkMode.current());
        int bgX = screenX - textWidth / 2 - pad;
        int bgY = screenY - textHeight / 2 - pad;
        c.emit(new GuideRenderPrimitive.FillRect(bgX, bgY, textWidth + pad * 2, textHeight + pad * 2, bgColor));
        c.emit(new GuideRenderPrimitive.DrawText(label, screenX - textWidth / 2, screenY - textHeight / 2, labelStyle));
    }

    private void emitNodesPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
        String rootNodeId = document.getNodeOrder()
            .isEmpty() ? null
                : document.getNodeOrder()
                    .get(0);

        for (var entry : layout.getNodePositions()
            .entrySet()) {
            String nodeId = entry.getKey();
            NodePosition pos = entry.getValue();
            FlowchartNode node = document.getNodes()
                .get(nodeId);
            if (node == null) continue;

            boolean isRoot = nodeId.equals(rootNodeId);
            int pdX = isRoot ? Math.max(1, Math.round(NODE_PADDING_X * 1.5f * activeZoom)) : paddingX;
            int pdY = isRoot ? Math.max(1, Math.round(NODE_PADDING_Y * 1.5f * activeZoom)) : paddingY;
            ResolvedTextStyle style = getOrScaleStyle(isRoot ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE, activeZoom);

            int sx = scaled(baseX, pos.getX(), activeZoom);
            int sy = scaled(baseY, pos.getY(), activeZoom);
            int sw = Math.max(1, Math.round(pos.getWidth() * activeZoom));
            int sh = Math.max(1, Math.round(pos.getHeight() * activeZoom));
            LytRect rect = new LytRect(sx, sy, sw, sh);

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
            FlowchartShapes.emitShape(c, node.getShape(), rect, colors.background(), colors.border());
            if (colors.accent() != MermaidNodeRenderer.DEFAULT_ACCENT
                && FlowchartShapes.hasAccentBar(node.getShape())) {
                c.emit(new GuideRenderPrimitive.FillRect(rect.x(), rect.y(), 3, rect.height(), colors.accent()));
            }

            int contentW = rect.width() - 2 * pdX;
            int contentH = rect.height() - 2 * pdY;
            LytRect contentArea = FlowchartShapes.contentBounds(rect, node.getShape(), contentW, contentH, pdX, pdY);
            int textY = contentArea.y();

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = Math.max(
                        1,
                        GuideText.measureWidth(badgeText, badgeStyle)
                            + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)) * 2);
                    int badgeHeight = Math.max(
                        1,
                        GuideText.lineHeight(badgeStyle)
                            + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)) * 2);
                    int badgeX = contentArea.x();
                    LytRect badge = new LytRect(badgeX, textY, badgeWidth, badgeHeight);
                    c.emit(new GuideRenderPrimitive.FillRect(
                        badge.x(), badge.y(), badge.width(), badge.height(),
                        MermaidNodeRenderer.BADGE_BACKGROUND));
                    c.emit(new GuideRenderPrimitive.DrawBorder(
                        badge.x(), badge.y(), badge.width(), badge.height(),
                        1, 1, 1, 1, MermaidNodeRenderer.BADGE_BORDER));
                    GuideText.emitText(
                        c,
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
                emitNodeContentPrimitives(c, contentLayout, contentArea, activeZoom);
            } else {
                String label = node.getLabel();
                if (label == null || label.isEmpty()) continue;

                List<String> lines = MermaidNodeRenderer.wrapText(new LayoutContext(new FontMetrics() {

                        @Override
                        public float getAdvance(int codePoint, ResolvedTextStyle s) {
                            return GuideText.measureWidth(new String(Character.toChars(codePoint)), s);
                        }

                        @Override
                        public int getLineHeight(ResolvedTextStyle s) {
                            return GuideText.lineHeight(s);
                        }
                    }), style, label, visibleWidth);
                int lineHeight = GuideText.lineHeight(style);
                int totalTextHeight = lines.size() * lineHeight;
                int textAreaHeight = contentArea.y() + visibleHeight - textY;
                int baseTextY = textY + Math.max(0, (textAreaHeight - totalTextHeight) / 2);
                for (int i = 0; i < lines.size(); i++) {
                    int lineWidth = GuideText.measureWidth(lines.get(i), style);
                    int textX = contentArea.x() + Math.max(0, (visibleWidth - lineWidth) / 2);
                    c.emit(new GuideRenderPrimitive.DrawText(lines.get(i), textX, baseTextY + i * lineHeight, style));
                }
            }
        }
    }

    @Nullable
    private static LytRect computeSubgraphBounds(FlowchartSubgraph subgraph, Map<String, NodePosition> positions) {
        LytRect result = null;
        for (String nodeId : subgraph.getNodeIds()) {
            NodePosition pos = positions.get(nodeId);
            if (pos != null) {
                LytRect nodeRect = new LytRect(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
                result = result != null ? LytRect.union(result, nodeRect) : nodeRect;
            }
        }
        for (var child : subgraph.getChildren()) {
            LytRect childBounds = computeSubgraphBounds(child, positions);
            if (childBounds != null) {
                result = result != null ? LytRect.union(result, childBounds) : childBounds;
            }
        }
        if (result != null) {
            String label = subgraph.getLabel();
            if (label != null && !label.isEmpty()) {
                result = new LytRect(
                    result.x(),
                    result.y() - SUBGRAPH_LABEL_HEIGHT,
                    result.width(),
                    result.height() + SUBGRAPH_LABEL_HEIGHT);
            }
        }
        return result;
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
                return 0xFF000000 | Integer.parseInt(h, 16);
            } else if (h.length() == 8) {
                return (int) Long.parseLong(h, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }
}
