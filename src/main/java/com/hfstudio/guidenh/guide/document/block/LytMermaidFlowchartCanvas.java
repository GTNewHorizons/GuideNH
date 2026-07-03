package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.EdgePath;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.NodePosition;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutStrategy;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartNode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
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
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int ICON_GAP_Y = 4;
    private static final int BADGE_PADDING_X = 4;
    private static final int BADGE_PADDING_Y = 2;
    private static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    private static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);
    private static final ConstantColor NODE_TEXT = new ConstantColor(0xFFD7DEE7);
    private static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(0xFFB8C2CF);
    private static final ConstantColor EDGE_COLOR = new ConstantColor(0xFF5D6C7C);

    private static final ResolvedTextStyle NODE_TEXT_STYLE = new ResolvedTextStyle(
        1f, false, false, false, false, false, false, false,
        null, NODE_TEXT, WhiteSpaceMode.NORMAL, TextAlignment.LEFT, false, null, false);
    private static final ResolvedTextStyle ICON_TEXT_STYLE = new ResolvedTextStyle(
        0.85f, false, false, false, false, false, false, false,
        null, ICON_TEXT_COLOR, WhiteSpaceMode.NORMAL, TextAlignment.LEFT, false, null, false);

    private final FlowchartDocument document;
    private final Map<String, LytBlock> nodeContentBlocks;
    private Map<String, NodeContentLayout> nodeContentLayouts = Map.of();
    private FlowchartLayoutResult layout;
    private int preferredWidth;
    private int preferredHeight;
    private int lastPickDocX;
    private int lastPickDocY;
    private boolean lastPickValid;
    @Nullable
    private LytParagraph lastFlowHoverParagraph;
    @Nullable
    private LytFlowContent lastFlowHoverContent;

    public LytMermaidFlowchartCanvas(FlowchartDocument document, Map<String, LytBlock> nodeContentBlocks) {
        this.document = document;
        this.nodeContentBlocks = nodeContentBlocks == null ? Collections.emptyMap()
            : new LinkedHashMap<>(nodeContentBlocks);
        for (LytBlock block : this.nodeContentBlocks.values()) {
            block.parent = this;
        }
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

    @Override
    protected void onPreRender() {
        refreshFlowHover();
    }

    public void setPreferredSize(int width, int height) {
        preferredWidth = Math.max(0, width);
        preferredHeight = Math.max(0, height);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int previousContentOffsetX = getRawOffsetX();
        int previousContentOffsetY = getRawOffsetY();
        int previousViewportWidth = Math.max(1, bounds.width() - CANVAS_PADDING * 2);
        int previousViewportHeight = Math.max(1, bounds.height() - CANVAS_PADDING * 2);
        int previousContentWidth = layout != null ? layout.getWidth() : 0;
        int previousContentHeight = layout != null ? layout.getHeight() : 0;

        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(preferredWidth, availableWidth))
            : Math.max(1, availableWidth);

        FlowchartLayoutStrategy strategy = FlowchartLayoutStrategy.forMode(document.getLayoutMode());
        layout = strategy.layout(document);

        Map<String, NodeContentLayout> layouts = new LinkedHashMap<>();
        for (var entry : layout.getNodePositions().entrySet()) {
            String nodeId = entry.getKey();
            LytBlock block = nodeContentBlocks.get(nodeId);
            if (block != null) {
                LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
                int contentWidth = Math.clamp(layout.getWidth() / 3, 96, 240);
                block.layout(localContext, 0, 0, contentWidth);
                LytRect visualBounds = resolveBlockVisualBounds(block);
                layouts.put(nodeId, new NodeContentLayout(block, visualBounds));
            }
        }
        nodeContentLayouts = layouts;

        int desiredHeight = (layout != null ? layout.getHeight() : 0) + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0 ? Math.max(48, preferredHeight)
            : Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, desiredHeight));
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);
        int innerViewportHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);

        restoreViewportAfterLayout(previousContentOffsetX, previousContentOffsetY,
            previousViewportWidth, previousViewportHeight,
            previousContentWidth, previousContentHeight,
            viewportWidth, innerViewportHeight);

        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    private void restoreViewportAfterLayout(int previousOffsetX, int previousOffsetY,
        int previousViewportWidth, int previousViewportHeight,
        int previousContentWidth, int previousContentHeight,
        int viewportWidth, int viewportHeight) {
        if (previousContentWidth <= 0 || previousContentHeight <= 0) {
            centerDiagram(viewportWidth, viewportHeight,
                layout.getWidth(), layout.getHeight());
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

    @Override
    public List<? extends LytNode> getChildren() {
        return new ArrayList<>(nodeContentBlocks.values());
    }

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
        ResolvedTextStyle style = getOrScaleStyle(NODE_TEXT_STYLE, activeZoom);
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));

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

            int accent = MermaidNodeRenderer.resolveAccentColor(node.getClasses(), node.getShape());
            int bg = MermaidNodeRenderer.DEFAULT_BACKGROUND;
            MermaidNodeRenderer.renderNode(context, rect, node.getShape(), bg, accent);
            MermaidNodeRenderer.renderAccentBar(context, rect, accent);

            int textY = rect.y() + paddingY;

            String icon = node.getIcon();
            if (icon != null) {
                String badgeText = MermaidNodeRenderer.simplifyIcon(icon);
                if (badgeText != null) {
                    int badgeWidth = Math.max(1, context.getStringWidth(badgeText, badgeStyle)
                        + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)) * 2);
                    int badgeHeight = Math.max(1, context.getLineHeight(badgeStyle)
                        + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)) * 2);
                    int badgeX = rect.x() + paddingX;
                    LytRect badge = new LytRect(badgeX, textY, badgeWidth, badgeHeight);
                    context.fillRect(badge, MermaidNodeRenderer.BADGE_BACKGROUND);
                    context.drawBorder(badge, MermaidNodeRenderer.BADGE_BORDER, 1);
                    context.drawText(badgeText, badge.x() + Math.max(2, Math.round(BADGE_PADDING_X * activeZoom)),
                        badge.y() + Math.max(1, Math.round(BADGE_PADDING_Y * activeZoom)), badgeStyle);
                    textY += badgeHeight + Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
                }
            }

            NodeContentLayout contentLayout = nodeContentLayouts.get(nodeId);
            if (contentLayout != null) {
                renderNodeContent(context, contentLayout, rect, paddingX, textY, activeZoom);
            } else {
                String label = node.getLabel();
                if (label == null || label.isEmpty()) continue;

                int lineWidth = context.getStringWidth(label, style);
                int textX = rect.x() + Math.max(paddingX, (rect.width() - lineWidth) / 2);
                context.drawText(label, textX, textY, style);
            }
        }
    }

    private void renderNodeContent(RenderContext context, NodeContentLayout contentLayout, LytRect rect,
        int paddingX, int contentY, float activeZoom) {
        LytRect innerViewport = getInnerViewport();
        LytRect contentViewport = resolveNodeContentRect(contentLayout, rect, paddingX, contentY, activeZoom);
        LytRect clip = intersect(innerViewport, contentViewport);
        if (clip == null) return;

        context.pushLocalScissor(clip);
        try {
            int originX = contentViewport.x() - Math.round(contentLayout.visualBounds().x() * activeZoom);
            int originY = contentViewport.y() - Math.round(contentLayout.visualBounds().y() * activeZoom);
            NodeContentRenderContext nodeContext = new NodeContentRenderContext(context, clip, originX, originY, activeZoom);
            renderNodeContentBlock(contentLayout.block(), nodeContext);
        } finally {
            context.popScissor();
        }
    }

    private void renderNodeContentBlock(LytBlock block, NodeContentRenderContext nodeContext) {
        if (block instanceof LytNode container && !container.getChildren().isEmpty()) {
            for (var child : new ArrayList<>(container.getChildren())) {
                if (child instanceof LytBlock childBlock) {
                    renderNodeContentBlock(childBlock, nodeContext);
                }
            }
            renderContainerDecoration(container, nodeContext);
        } else if (usesRawGl(block)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(nodeContext.getDocumentOriginX(), nodeContext.getDocumentOriginY(), 0f);
            GL11.glScalef(nodeContext.getScale(), nodeContext.getScale(), 1f);
            try {
                block.render(nodeContext);
            } finally {
                GL11.glPopMatrix();
            }
        } else {
            block.render(nodeContext);
        }
    }

    private void refreshFlowHover() {
        if (!lastPickValid || layout == null) return;
        NodeHit hit = pickNodeHit(lastPickDocX, lastPickDocY);
        LytFlowContent hoveredFlow = null;
        LytParagraph hoveredParagraph = null;
        if (hit != null) {
            for (var content : hit.flowPath().targets()) {
                if (content instanceof InteractiveElement) {
                    hoveredFlow = content;
                    break;
                }
            }
            if (hoveredFlow != null) {
                for (LytNode node = hit.node(); node != null; node = node.getParent()) {
                    if (node instanceof LytParagraph p) {
                        hoveredParagraph = p;
                        break;
                    }
                }
            }
        }
        if (hoveredParagraph != lastFlowHoverParagraph || hoveredFlow != lastFlowHoverContent) {
            if (lastFlowHoverParagraph != null) lastFlowHoverParagraph.onMouseLeave();
            if (hoveredParagraph != null) hoveredParagraph.onMouseEnter(hoveredFlow);
            lastFlowHoverParagraph = hoveredParagraph;
            lastFlowHoverContent = hoveredFlow;
        }
    }

    @Nullable
    private NodeHit pickNodeHit(int documentX, int documentY) {
        if (layout == null) return null;
        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();

        for (var entry : layout.getNodePositions().entrySet()) {
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

            if (contentScreenRect == null || !contentScreenRect.contains(documentX, documentY)) continue;

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
        FlowchartNode node = document.getNodes().get(nodeId);
        if (node == null || node.getIcon() == null) return 0;
        String badgeText = MermaidNodeRenderer.simplifyIcon(node.getIcon());
        if (badgeText == null) return 0;
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        return contextLineHeight(badgeStyle) + badgePaddingY * 2 + iconGapY;
    }

    private int unscaleCoordinate(int coordinate, float activeZoom) {
        return Math.max(0, Math.round(coordinate / Math.max(activeZoom, 0.0001f)));
    }

    private int contextLineHeight(ResolvedTextStyle style) {
        return Math.max(1, Math.round((9 + 1) * style.fontScale()));
    }

    private LytRect resolveNodeContentRect(NodeContentLayout contentLayout, LytRect nodeRect, int paddingX,
        int contentY, float activeZoom) {
        return new LytRect(
            nodeRect.x() + paddingX,
            contentY,
            Math.max(1, Math.round(contentLayout.visualBounds().width() * activeZoom)),
            Math.max(1, Math.round(contentLayout.visualBounds().height() * activeZoom)));
    }

    @Override
    public LytNode pickNode(int x, int y) {
        if (!getBounds().contains(x, y)) return null;
        lastPickDocX = x;
        lastPickDocY = y;
        lastPickValid = true;
        NodeHit hit = pickNodeHit(x, y);
        return hit != null ? hit.node() : this;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (layout == null || !getInnerViewport().contains(x, y)) return false;
        NodeHit hit = pickNodeHit(x, y);
        if (hit == null) return false;
        boolean handled = false;
        for (var content : hit.flowPath().targets()) {
            if (content instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
                if (handled) return true;
            }
        }
        for (LytNode current = hit.node(); current != null && current != this && !handled; current = current.getParent()) {
            if (current instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
            }
        }
        return handled;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (layout == null || !getInnerViewport().contains((int) x, (int) y)) return Optional.empty();
        NodeHit hit = pickNodeHit((int) x, (int) y);
        if (hit == null) return Optional.empty();
        for (var content : hit.flowPath().targets()) {
            if (content instanceof InteractiveElement interactiveElement) {
                Optional<GuideTooltip> tooltip = interactiveElement.getTooltip(hit.localX(), hit.localY());
                if (tooltip.isPresent()) return tooltip;
            }
        }
        for (LytNode current = hit.node(); current != null && current != this; current = current.getParent()) {
            if (current instanceof InteractiveElement interactiveElement) {
                Optional<GuideTooltip> tooltip = interactiveElement.getTooltip(hit.localX(), hit.localY());
                if (tooltip.isPresent()) return tooltip;
            }
        }
        return Optional.empty();
    }

    // ---- Static helpers ----

    private static boolean usesRawGl(LytBlock block) {
        return block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock
            || block instanceof LytItemImage
            || block instanceof LytNeiRecipeBox;
    }

    private static boolean containsScene(LytBlock block) {
        if (block == null) return false;
        if (block instanceof LytGuidebookScene) return true;
        if (block instanceof LytNode container) {
            for (var child : container.getChildren()) {
                if (child instanceof LytBlock childBlock && containsScene(childBlock)) return true;
            }
        }
        return false;
    }

    private static void renderContainerDecoration(LytNode container, RenderContext context) {
        if (!(container instanceof LytBox box)) return;
        LytRect b = container.getBounds();
        if (box.getBackgroundColor() != null) {
            context.fillRect(b, box.getBackgroundColor());
        }
        int topW = box.getBorderTop().width();
        int bottomW = box.getBorderBottom().width();
        if (topW > 0) {
            context.fillRect(
                b.x(), b.y(), b.width(), topW,
                context.resolveColor(box.getBorderTop().color()));
        }
        if (bottomW > 0) {
            context.fillRect(
                b.x(), b.bottom() - bottomW, b.width(), bottomW,
                context.resolveColor(box.getBorderBottom().color()));
        }
    }

    private static LytRect resolveBlockVisualBounds(LytBlock block) {
        LytRect[] result = { LytRect.empty() };
        block.visit(new LytVisitor() {
            @Override
            public LytVisitor.Result beforeNode(LytNode node) {
                if (node instanceof LytBlock childBlock) {
                    result[0] = LytRect.union(result[0], resolveSelfVisualBounds(childBlock));
                }
                return LytVisitor.Result.CONTINUE;
            }
        });
        return result[0];
    }

    private static LytRect resolveSelfVisualBounds(LytBlock block) {
        LytRect bounds = block.getBounds();
        if (bounds == null) return LytRect.empty();
        if (block instanceof LytLatexBlock latexBlock) return latexBlock.getVisualBounds();
        if (block instanceof LytLatexDisplayBlock latexDisplayBlock) return latexDisplayBlock.getVisualBounds();
        return bounds;
    }

    @Nullable
    private static LytRect intersect(LytRect a, LytRect b) {
        int left = Math.max(a.x(), b.x());
        int top = Math.max(a.y(), b.y());
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (right <= left || bottom <= top) return null;
        return new LytRect(left, top, right - left, bottom - top);
    }

    // ---- Inner classes ----

    public static class NodeContentLayout {
        private final LytBlock block;
        private final LytRect visualBounds;

        public NodeContentLayout(LytBlock block, LytRect visualBounds) {
            this.block = block;
            this.visualBounds = visualBounds != null && !visualBounds.isEmpty() ? visualBounds : LytRect.empty();
        }

        public LytBlock block() { return block; }
        public LytRect visualBounds() { return visualBounds; }
    }

    public static class NodeHit {
        private final LytNode node;
        private final FlowInteractionPath flowPath;
        private final int localX;
        private final int localY;

        public NodeHit(LytNode node, @Nullable FlowInteractionPath flowPath, int localX, int localY) {
            this.node = node;
            this.flowPath = flowPath != null ? flowPath : FlowInteractionPath.empty();
            this.localX = localX;
            this.localY = localY;
        }

        public LytNode node() { return node; }
        public FlowInteractionPath flowPath() { return flowPath; }
        public int localX() { return localX; }
        public int localY() { return localY; }
    }

    public static class NodeContentRenderContext implements RenderContext {

        private final RenderContext delegate;
        private final LytRect viewport;
        private final int originX;
        private final int originY;
        private final float scale;
        private final Map<ResolvedTextStyle, ResolvedTextStyle> scaledStyleCache = new IdentityHashMap<>();

        public NodeContentRenderContext(RenderContext delegate, LytRect viewport, int originX, int originY,
            float scale) {
            this.delegate = delegate;
            this.viewport = new LytRect(
                0, 0,
                Math.max(1, Math.round(viewport.width() / scale)),
                Math.max(1, Math.round(viewport.height() / scale)));
            this.originX = originX;
            this.originY = originY;
            this.scale = Math.max(0.0001f, scale);
        }

        public float getScale() { return scale; }

        @Override
        public LightDarkMode lightDarkMode() { return delegate.lightDarkMode(); }

        @Override
        public LytRect viewport() { return viewport; }

        @Override
        public int getDocumentOriginX() { return originX; }

        @Override
        public int getDocumentOriginY() { return originY; }

        @Override
        public LytRect toScreenRect(LytRect rect) {
            LytRect s = scaleRect(rect);
            return new LytRect(
                s.x() + delegate.getDocumentOriginX(),
                s.y() + delegate.getDocumentOriginY() - delegate.getScrollOffsetY(),
                s.width(), s.height());
        }

        @Override
        public int resolveColor(ColorValue ref) { return delegate.resolveColor(ref); }

        @Override
        public void fillRect(LytRect rect, int argbColor) {
            delegate.fillRect(scaleRect(rect), argbColor);
        }

        @Override
        public void fillRect(int x, int y, int width, int height, int argbColor) {
            delegate.fillRect(scaleX(x), scaleY(y), scaleLength(width), scaleLength(height), argbColor);
        }

        @Override
        public void drawBorder(LytRect rect, int argbColor, int thickness) {
            delegate.drawBorder(scaleRect(rect), argbColor, Math.max(1, scaleLength(thickness)));
        }

        @Override
        public void drawBorder(int x, int y, int width, int height, int argbColor, int thickness) {
            delegate.drawBorder(scaleX(x), scaleY(y), scaleLength(width), scaleLength(height),
                argbColor, Math.max(1, scaleLength(thickness)));
        }

        @Override
        public void drawText(String text, int x, int y, ResolvedTextStyle style) {
            delegate.drawText(text, scaleX(x), scaleY(y), scaleStyle(style));
        }

        @Override
        public int getStringWidth(String text, ResolvedTextStyle style) {
            return scaleLength(delegate.getStringWidth(text, style));
        }

        @Override
        public int getLineHeight(ResolvedTextStyle style) {
            return scaleLength(delegate.getLineHeight(style));
        }

        @Override
        public void renderItem(ItemStack stack, int x, int y) {
            renderScaledItem(stack, x, y, true);
        }

        @Override
        public void renderItemIcon(ItemStack stack, int x, int y) {
            renderScaledItem(stack, x, y, false);
        }

        private void renderScaledItem(ItemStack stack, int x, int y, boolean overlay) {
            int screenX = scaleX(x);
            int screenY = scaleY(y);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(screenX, screenY, 0f);
                GL11.glScalef(scale, scale, 1f);
                if (overlay) delegate.renderItem(stack, 0, 0);
                else delegate.renderItemIcon(stack, 0, 0);
            } finally {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void blitGuiSprite(LytRect rect, GuiSprite sprite) {
            if (sprite == null) return;
            int sx = scaleX(rect.x());
            int sy = scaleY(rect.y());
            GL11.glPushMatrix();
            GL11.glTranslatef(sx, sy, 0f);
            GL11.glScalef(scale, scale, 1f);
            try {
                delegate.blitTexture(
                    sprite.getTexture(), 0, 0,
                    sprite.getU(), sprite.getV(),
                    sprite.getWidth(), sprite.getHeight());
            } finally {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void fillIcon(LytRect rect, GuiSprite sprite, ColorValue color) {
            delegate.fillIcon(scaleRect(rect), sprite, color);
        }

        @Override
        public void blitTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
            delegate.blitTexture(texture, scaleX(x), scaleY(y), u, v, scaleLength(width), scaleLength(height));
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, float thickness, int argbColor) {
            delegate.drawLine(scaleFloatX(x1), scaleFloatY(y1), scaleFloatX(x2), scaleFloatY(y2),
                Math.max(1f, thickness * scale), argbColor);
        }

        @Override
        public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
            delegate.fillTriangle(scaleFloatX(x1), scaleFloatY(y1), scaleFloatX(x2), scaleFloatY(y2),
                scaleFloatX(x3), scaleFloatY(y3), argbColor);
        }

        @Override
        public void fillPolygon(float[] xs, float[] ys, int argbColor) {
            float[] scaledXs = new float[xs.length];
            float[] scaledYs = new float[ys.length];
            for (int i = 0; i < xs.length; i++) {
                scaledXs[i] = scaleFloatX(xs[i]);
                scaledYs[i] = scaleFloatY(ys[i]);
            }
            delegate.fillPolygon(scaledXs, scaledYs, argbColor);
        }

        @Override
        public void fillCircle(float cx, float cy, float radius, int argbColor) {
            delegate.fillCircle(scaleFloatX(cx), scaleFloatY(cy), radius * scale, argbColor);
        }

        @Override
        public void drawCircleOutline(float cx, float cy, float radius, float thickness, int argbColor) {
            delegate.drawCircleOutline(scaleFloatX(cx), scaleFloatY(cy), radius * scale,
                Math.max(1f, thickness * scale), argbColor);
        }

        @Override
        public void pushScissor(LytRect rect) {
            delegate.pushScissor(scaleRect(rect));
        }

        @Override
        public void pushLocalScissor(LytRect rect) {
            delegate.pushScissor(scaleRect(rect));
        }

        @Override
        public LytRect currentScissor() { return delegate.currentScissor(); }

        @Override
        public void popScissor() { delegate.popScissor(); }

        @Override
        public void restoreExternalRenderState() { delegate.restoreExternalRenderState(); }

        @Override
        public void beginLocalView() {
            GL11.glPushMatrix();
            GL11.glTranslatef(originX, originY, 0f);
            GL11.glScalef(scale, scale, 1f);
        }

        @Override
        public void endLocalView() {
            GL11.glPopMatrix();
        }

        private ResolvedTextStyle scaleStyle(ResolvedTextStyle style) {
            return scaledStyleCache.computeIfAbsent(style, key -> new ResolvedTextStyle(
                key.fontScale() * scale, key.bold(), key.italic(), key.underlined(),
                key.wavyUnderline(), key.dottedUnderline(), key.strikethrough(), key.obfuscated(),
                key.font(), key.color(), key.whiteSpace(), key.alignment(),
                key.dropShadow(), key.backgroundColor(), key.inlineCode()));
        }

        private LytRect scaleRect(LytRect rect) {
            return new LytRect(scaleX(rect.x()), scaleY(rect.y()), scaleLength(rect.width()), scaleLength(rect.height()));
        }

        private int scaleX(int x) { return originX + Math.round(x * scale); }
        private int scaleY(int y) { return originY + Math.round(y * scale); }
        private int scaleLength(int value) { return Math.max(1, Math.round(value * scale)); }
        private float scaleFloatX(float x) { return originX + x * scale; }
        private float scaleFloatY(float y) { return originY + y * scale; }
    }
}
