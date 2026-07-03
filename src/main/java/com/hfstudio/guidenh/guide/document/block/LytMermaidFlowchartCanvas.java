package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.EdgePath;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodePosition;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytMermaidFlowchartCanvas extends LytMermaidCanvas<LytMermaidFlowchartCanvas> {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 120;
    private static final int MAX_HEIGHT = 320;
    private static final int CONNECTOR_THICKNESS = 2;
    private static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    private static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);
    private static final ConstantColor NODE_BACKGROUND = new ConstantColor(0xFF1F2A38);
    private static final ConstantColor NODE_BORDER = new ConstantColor(0xFF5D6C7C);
    private static final ConstantColor NODE_TEXT = new ConstantColor(0xFFD7DEE7);
    private static final ConstantColor EDGE_COLOR = new ConstantColor(0xFF5D6C7C);

    private static final ResolvedTextStyle NODE_TEXT_STYLE = new ResolvedTextStyle(
        1f, false, false, false, false, false, false, false,
        null, NODE_TEXT, WhiteSpaceMode.NORMAL, TextAlignment.LEFT, false, null, false);

    private final FlowchartDocument document;
    private FlowchartLayoutResult layout;
    private int preferredWidth;
    private int preferredHeight;

    public LytMermaidFlowchartCanvas(FlowchartDocument document) {
        this.document = document;
    }

    @Override
    public int canvasPadding() { return CANVAS_PADDING; }

    @Override
    public int contentWidth() { return layout != null ? layout.getWidth() : 0; }

    @Override
    public int contentHeight() { return layout != null ? layout.getHeight() : 0; }

    @Override
    public int contentOriginX() { return 0; }

    @Override
    public int contentOriginY() { return 0; }

    @Override
    protected boolean diagramReady() { return layout != null; }

    @Override
    protected void renderPanel(RenderContext context) {
        context.fillRect(bounds, context.resolveColor(PANEL_BACKGROUND));
        context.drawBorder(bounds, context.resolveColor(PANEL_BORDER), 1);
    }

    @Override
    protected void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom) {
        renderEdges(context, baseX, baseY, activeZoom);
        renderNodes(context, baseX, baseY, activeZoom);
    }

    public void setPreferredSize(int width, int height) {
        preferredWidth = Math.max(0, width);
        preferredHeight = Math.max(0, height);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(preferredWidth, availableWidth))
            : Math.max(1, availableWidth);

        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(document.getLayoutMode());
        layout = strategy.layout(document);

        int desiredHeight = (layout != null ? layout.getHeight() : 0) + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, desiredHeight));
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);

        if (layout != null) {
            centerDiagram(layout.getWidth(), layout.getHeight());
        }

        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    private void renderEdges(RenderContext context, int baseX, int baseY, float activeZoom) {
        int edgeColor = context.resolveColor(EDGE_COLOR);
        for (EdgePath edge : layout.getEdgePaths()) {
            List<FlowchartLayoutResult.Point> points = edge.getPoints();
            for (int i = 1; i < points.size(); i++) {
                FlowchartLayoutResult.Point from = points.get(i - 1);
                FlowchartLayoutResult.Point to = points.get(i);
                context.drawLine(
                    scaled(baseX, from.getX(), activeZoom),
                    scaled(baseY, from.getY(), activeZoom),
                    scaled(baseX, to.getX(), activeZoom),
                    scaled(baseY, to.getY(), activeZoom),
                    CONNECTOR_THICKNESS,
                    edgeColor);
            }
        }
    }

    private void renderNodes(RenderContext context, int baseX, int baseY, float activeZoom) {
        int bgColor = context.resolveColor(NODE_BACKGROUND);
        int borderColor = context.resolveColor(NODE_BORDER);
        ResolvedTextStyle style = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);

        for (var entry : layout.getNodePositions().entrySet()) {
            String nodeId = entry.getKey();
            NodePosition pos = entry.getValue();
            FlowchartNode node = document.getNodes().get(nodeId);
            if (node == null) continue;

            int sx = scaled(baseX, pos.getX(), activeZoom);
            int sy = scaled(baseY, pos.getY(), activeZoom);
            int sw = Math.max(1, Math.round(pos.getWidth() * activeZoom));
            int sh = Math.max(1, Math.round(pos.getHeight() * activeZoom));
            LytRect rect = new LytRect(sx, sy, sw, sh);

            MermaidNodeRenderer.renderNode(context, rect, node.getShape(), bgColor, borderColor);

            String label = node.getLabel();
            if (label != null && !label.isEmpty()) {
                int textX = sx + Math.max(2, Math.round(4 * activeZoom));
                int textY = sy + Math.max(2, Math.round(4 * activeZoom));
                context.drawText(label, textX, textY, style);
            }
        }
    }

    @Override
    public LytNode pickNode(int x, int y) {
        if (!getBounds().contains(x, y)) return null;
        return this;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        return false;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }
}
