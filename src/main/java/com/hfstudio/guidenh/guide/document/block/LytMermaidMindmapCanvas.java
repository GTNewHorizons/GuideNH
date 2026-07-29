package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapLayoutMode;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapNode;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

public class LytMermaidMindmapCanvas extends LytMermaidCanvas<LytMermaidMindmapCanvas> implements DebugComponent {

    private static final int CANVAS_PADDING = 10;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 170;
    private static final int MAX_HEIGHT = 320;
    private static final int NODE_PADDING_X = 10;
    private static final int NODE_PADDING_Y = 6;
    private static final int NODE_GAP_X = 32;
    private static final int NODE_GAP_Y = 14;
    private static final int ICON_GAP_Y = 4;
    private static final int CONNECTOR_THICKNESS = 1;
    static final ConstantColor ROOT_TEXT_COLOR = new ConstantColor(0xFFF1F6FB);
    static final ConstantColor NODE_TEXT_COLOR = new ConstantColor(0xFFD7DEE7);
    static final ConstantColor ICON_TEXT_COLOR = new ConstantColor(0xFFB8C2CF);

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
        false,
        0.0f);
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
        NODE_TEXT_COLOR,
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false,
        0.0f);
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
        false,
        0.0f);

    private final MindmapDocument mindmap;

    private DiagramLayout layout;
    private int precomputedLayoutWidth;

    public LytMermaidMindmapCanvas(MindmapDocument mindmap, Map<String, LytBlock> nodeContentBlocks) {
        this.mindmap = mindmap;
        initNodeContentBlocks(nodeContentBlocks);
    }

    @Override
    public int canvasPadding() {
        return CANVAS_PADDING;
    }

    @Override
    public int contentWidth() {
        return layout != null ? layout.diagramWidth() : 0;
    }

    @Override
    public int contentHeight() {
        return layout != null ? layout.diagramHeight() : 0;
    }

    @Override
    public int contentOriginX() {
        return layout != null ? layout.contentBounds()
            .x() : 0;
    }

    @Override
    public int contentOriginY() {
        return layout != null ? layout.contentBounds()
            .y() : 0;
    }

    @Override
    public boolean diagramReady() {
        return layout != null;
    }

    public MindmapDocument getMindmap() {
        return mindmap;
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
        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(preferredWidth, availableWidth))
            : Math.max(1, availableWidth);
        layout = buildLayout(context, safeWidth);
        int desiredHeight = layout.diagramHeight() + CANVAS_PADDING * 2;
        int viewportHeight = preferredHeight > 0
            ? Math.max(48, Math.max(preferredHeight, desiredHeight))
            : Math.max(MIN_HEIGHT, desiredHeight);
        int viewportWidth = Math.max(1, safeWidth - CANVAS_PADDING * 2);
        int innerViewportHeight = Math.max(1, viewportHeight - CANVAS_PADDING * 2);
        restoreViewportAfterLayout(
            layout,
            previousContentOffsetX,
            previousContentOffsetY,
            previousViewportWidth,
            previousViewportHeight,
            viewportWidth,
            innerViewportHeight);
        return new LytRect(x, y, safeWidth, viewportHeight);
    }

    /**
     * Pre-compute diagram layout before the first Rust layout pass and set
     * preferredHeight so Rust allocates the correct canvas height immediately.
     * Caches the layout result for reuse in afterExternalLayout when the
     * actual bounds width matches the pre-computation width.
     *
     * @param ctx            LayoutContext backed by GuideText-based FontMetrics
     * @param availableWidth estimated canvas content width (page width or
     *                       placeholder width)
     */
    public void precomputeLayout(LayoutContext ctx, int availableWidth) {
        int safeWidth = preferredWidth > 0
            ? Math.max(1, Math.min(preferredWidth, availableWidth))
            : Math.max(1, availableWidth);
        this.layout = buildLayout(ctx, safeWidth);
        this.precomputedLayoutWidth = safeWidth;
        if (layout != null) {
            int desiredHeight = layout.diagramHeight() + CANVAS_PADDING * 2;
            int newPreferredHeight = preferredHeight > 0
                ? Math.max(48, Math.max(preferredHeight, desiredHeight))
                : Math.max(MIN_HEIGHT, desiredHeight);
            preferredHeight = newPreferredHeight;
            int diagramWidth = layout.diagramWidth() + CANVAS_PADDING * 2;
            preferredWidth = diagramWidth;
            GuideDebugLog.debugAlways(
                "[GuideNH-Mermaid] precomputeLayout OK diagramHeight={} preferredHeight={}",
                layout.diagramHeight(), preferredHeight);
            GuideDebugLog.debugAlways(
                "[GuideNH-Mermaid] precomputeLayout set explicitWidth={} diagramWidth={} safeWidth={}",
                preferredWidth, layout.diagramWidth(), safeWidth);
        } else {
            GuideDebugLog.debugAlways(
                "[GuideNH-Mermaid] precomputeLayout FAILED layout=null safeWidth={}",
                safeWidth);
        }
    }

    @Override
    protected void afterExternalLayout() {
        int safeWidth = preferredWidth > 0
            ? Math.max(1, Math.min(preferredWidth, Math.max(1, bounds.width())))
            : Math.max(1, bounds.width());

        GuideDebugLog.debugAlways(
            "[GuideNH-Mermaid] afterExternalLayout entered layout={} safeWidth={} precomputedLayoutWidth={} bounds.height={}",
            layout != null, safeWidth, precomputedLayoutWidth, bounds.height());

        // Phase 1: ensure layout result matches the actual canvas width.
        // Width matches precompute → reuse cached layout (no recompute).
        // Width mismatch or no precompute → recompute at the correct width.
        if (layout == null || precomputedLayoutWidth <= 0 || precomputedLayoutWidth != safeWidth) {
            LayoutContext fallbackCtx = new LayoutContext(new FontMetrics() {
                @Override
                public float getAdvance(int codePoint, ResolvedTextStyle s) {
                    return GuideText.measureWidth(new String(Character.toChars(codePoint)), s);
                }
                @Override
                public int getLineHeight(ResolvedTextStyle s) {
                    return GuideText.lineHeight(s);
                }
            });
            layout = buildLayout(fallbackCtx, safeWidth);
            GuideDebugLog.debugAlways(
                "[GuideNH-Mermaid] afterExternalLayout recomputed layout={}",
                layout != null);
        }

        // Phase 2: if layout is valid, correct bounds height if needed (兜底).
        if (layout != null) {
            int desiredHeight = layout.diagramHeight() + CANVAS_PADDING * 2;
            int expectedHeight = preferredHeight > 0
                ? Math.max(48, Math.max(preferredHeight, desiredHeight))
                : Math.max(MIN_HEIGHT, desiredHeight);
            if (bounds.height() != expectedHeight) {
                GuideDebugLog.debugAlways(
                    "[GuideNH-Mermaid] afterExternalLayout correcting bounds height {} -> {}",
                    bounds.height(), expectedHeight);
                bounds = new LytRect(bounds.x(), bounds.y(), bounds.width(), expectedHeight);
            }
        }

        GuideDebugLog.debugAlways(
            "[GuideNH-Mermaid] afterExternalLayout exit layout={} bounds.height={}",
            layout != null, bounds.height());
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    // ---- primitives pipeline (replaces render* for the primitives path) ----

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    protected void emitDiagramPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {
        emitConnectorsPrimitives(c, layout.root(), baseX, baseY, activeZoom);
        emitNodesPrimitives(c, layout.root(), baseX, baseY, activeZoom);
    }

    private void emitConnectorsPrimitives(PrimitiveCollector c, NodeLayout node, int baseX, int baseY,
        float activeZoom) {
        for (NodeLayout child : node.children) {
            if (mindmap.getLayoutMode() == MindmapLayoutMode.TIDY_TREE) {
                emitVerticalConnector(
                    c,
                    scaled(baseX, node.centerX(), activeZoom),
                    scaled(baseY, node.bottom(), activeZoom),
                    scaled(baseX, child.centerX(), activeZoom),
                    scaled(baseY, child.y, activeZoom),
                    0xFF5D6C7C);
            } else {
                boolean rightSide = child.centerX() >= node.centerX();
                int parentEdgeX = scaled(baseX, rightSide ? node.right() : node.x, activeZoom);
                int childEdgeX = scaled(baseX, rightSide ? child.x : child.right(), activeZoom);
                emitHorizontalConnector(
                    c,
                    parentEdgeX,
                    scaled(baseY, node.centerY(), activeZoom),
                    childEdgeX,
                    scaled(baseY, child.centerY(), activeZoom),
                    0xFF5D6C7C);
            }
            emitConnectorsPrimitives(c, child, baseX, baseY, activeZoom);
        }
    }

    private void emitHorizontalConnector(PrimitiveCollector c, int startX, int startY, int endX, int endY, int color) {
        int midX = (startX + endX) / 2;
        emitHorizontalLine(c, startX, midX, startY, color);
        emitVerticalLine(c, midX, startY, endY, color);
        emitHorizontalLine(c, midX, endX, endY, color);
    }

    private void emitVerticalConnector(PrimitiveCollector c, int startX, int startY, int endX, int endY, int color) {
        int midY = (startY + endY) / 2;
        emitVerticalLine(c, startX, startY, midY, color);
        emitHorizontalLine(c, startX, endX, midY, color);
        emitVerticalLine(c, endX, midY, endY, color);
    }

    private void emitHorizontalLine(PrimitiveCollector c, int startX, int endX, int y, int color) {
        int left = Math.min(startX, endX);
        int width = Math.abs(endX - startX) + 1;
        c.emit(new GuideRenderPrimitive.FillRect(left, y, width, CONNECTOR_THICKNESS, color));
    }

    private void emitVerticalLine(PrimitiveCollector c, int x, int startY, int endY, int color) {
        int top = Math.min(startY, endY);
        int height = Math.abs(endY - startY) + 1;
        c.emit(new GuideRenderPrimitive.FillRect(x, top, CONNECTOR_THICKNESS, height, color));
    }

    private void emitNodesPrimitives(PrimitiveCollector c, NodeLayout node, int baseX, int baseY, float activeZoom) {
        LytRect rect = new LytRect(
            scaled(baseX, node.x, activeZoom),
            scaled(baseY, node.y, activeZoom),
            Math.max(1, Math.round(node.width * activeZoom)),
            Math.max(1, Math.round(node.height * activeZoom)));
        LytRect boxRect = rect;
        NodeColors colors = resolveColors(node.node);
        c.emit(new GuideRenderPrimitive.FillRect(boxRect.x(), boxRect.y(), boxRect.width(), boxRect.height(),
            colors.background));
        int borderThickness = node.node.getShape() == MermaidNodeShape.BANG ? 2 : 1;
        c.emit(new GuideRenderPrimitive.DrawBorder(
            boxRect.x(), boxRect.y(), boxRect.width(), boxRect.height(),
            borderThickness, borderThickness, borderThickness, borderThickness, colors.border));
        c.emit(new GuideRenderPrimitive.FillRect(boxRect.x(), boxRect.y(), 3, boxRect.height(), colors.accent));

        ResolvedTextStyle style = getOrScaleStyle(node.depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE, activeZoom);
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        int badgePaddingX = Math.max(2, Math.round(4 * activeZoom));
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int textY = rect.y() + paddingY;
        if (node.showBadge && node.badgeText != null) {
            int badgeWidth = Math.max(1, GuideText.measureWidth(node.badgeText, badgeStyle) + badgePaddingX * 2);
            int badgeHeight = Math.max(1, GuideText.lineHeight(badgeStyle) + badgePaddingY * 2);
            LytRect badge = new LytRect(
                rect.x() + paddingX,
                textY,
                badgeWidth,
                badgeHeight);
            c.emit(new GuideRenderPrimitive.FillRect(
                badge.x(), badge.y(), badge.width(), badge.height(),
                MermaidNodeRenderer.BADGE_BACKGROUND));
            c.emit(new GuideRenderPrimitive.DrawBorder(
                badge.x(), badge.y(), badge.width(), badge.height(),
                1, 1, 1, 1, MermaidNodeRenderer.BADGE_BORDER));
            GuideText.emitText(c, node.badgeText, badge.x() + badgePaddingX, badge.y() + badgePaddingY, badgeStyle);
            textY = badge.bottom() + iconGapY;
        }

        if (node.contentLayout != null) {
            LytRect contentViewport = resolveNodeContentRect(node.contentLayout, rect, paddingX, textY, activeZoom);
            emitNodeContentPrimitives(c, node.contentLayout.block(), contentViewport,
                node.contentLayout.visualBounds(), activeZoom);
        } else {
            int lineHeight = GuideText.lineHeight(style);
            for (String line : node.lines) {
                int lineWidth = GuideText.measureWidth(line, style);
                int textX = rect.x() + Math.max(paddingX, (rect.width() - lineWidth) / 2);
                GuideText.emitText(c, line, textX, textY, style);
                textY += lineHeight;
            }
        }

        for (NodeLayout child : node.children) {
            emitNodesPrimitives(c, child, baseX, baseY, activeZoom);
        }
    }

    private DiagramLayout buildLayout(LayoutContext context, int availableWidth) {
        int innerWidth = Math.max(72, availableWidth - CANVAS_PADDING * 2);
        int maxNodeTextWidth = Math.clamp(innerWidth / 3, 72, 180);
        NodeLayout root = prepareLayout(context, mindmap.getRoot(), 0, maxNodeTextWidth);

        if (mindmap.getLayoutMode() == MindmapLayoutMode.TIDY_TREE) {
            measureTopDown(root);
            layoutTopDown(root, 0, 0);
            return buildDiagramLayout(root);
        }

        List<NodeLayout> leftChildren = new ArrayList<>();
        List<NodeLayout> rightChildren = new ArrayList<>();
        for (int i = 0; i < root.children.size(); i++) {
            if ((i & 1) == 0) {
                rightChildren.add(root.children.get(i));
            } else {
                leftChildren.add(root.children.get(i));
            }
        }

        int leftWidth = 0;
        int leftHeight = 0;
        for (NodeLayout child : leftChildren) {
            measureSideTree(child);
            leftWidth = Math.max(leftWidth, child.subtreeWidth);
            leftHeight += child.subtreeHeight;
        }
        if (leftChildren.size() > 1) {
            leftHeight += NODE_GAP_Y * (leftChildren.size() - 1);
        }

        int rightWidth = 0;
        int rightHeight = 0;
        for (NodeLayout child : rightChildren) {
            measureSideTree(child);
            rightWidth = Math.max(rightWidth, child.subtreeWidth);
            rightHeight += child.subtreeHeight;
        }
        if (rightChildren.size() > 1) {
            rightHeight += NODE_GAP_Y * (rightChildren.size() - 1);
        }

        int leftGap = leftWidth > 0 ? NODE_GAP_X : 0;
        int rightGap = rightWidth > 0 ? NODE_GAP_X : 0;
        int diagramWidth = leftWidth + leftGap + root.width + rightGap + rightWidth;
        int diagramHeight = Math.max(root.height, Math.max(leftHeight, rightHeight));
        int rootX = leftWidth + leftGap;
        int rootCenterY = diagramHeight / 2;
        root.x = rootX;
        root.y = rootCenterY - root.height / 2;

        int rightAnchorX = root.x + root.width + NODE_GAP_X;
        int rightCursorY = rootCenterY - rightHeight / 2;
        for (NodeLayout child : rightChildren) {
            int childCenterY = rightCursorY + child.subtreeHeight / 2;
            layoutSideTree(child, rightAnchorX, childCenterY, true);
            rightCursorY += child.subtreeHeight + NODE_GAP_Y;
        }

        int leftAnchorX = root.x - NODE_GAP_X;
        int leftCursorY = rootCenterY - leftHeight / 2;
        for (NodeLayout child : leftChildren) {
            int childCenterY = leftCursorY + child.subtreeHeight / 2;
            layoutSideTree(child, leftAnchorX, childCenterY, false);
            leftCursorY += child.subtreeHeight + NODE_GAP_Y;
        }

        return buildDiagramLayout(root);
    }

    private DiagramLayout buildDiagramLayout(NodeLayout root) {
        LytRect contentBounds = collectRenderedBounds(root);
        return new DiagramLayout(
            root,
            Math.max(1, contentBounds.width()),
            Math.max(1, contentBounds.height()),
            contentBounds,
            collectContentNodes(root, new ArrayList<>()));
    }

    private LytRect collectRenderedBounds(NodeLayout node) {
        LytRect bounds = resolveNodeVisualRect(node);
        for (NodeLayout child : node.children) {
            bounds = LytRect.union(bounds, collectRenderedBounds(child));
        }
        return bounds;
    }

    private List<NodeLayout> collectContentNodes(NodeLayout node, List<NodeLayout> result) {
        if (node.contentLayout != null) {
            result.add(node);
        }
        for (NodeLayout child : node.children) {
            collectContentNodes(child, result);
        }
        return result;
    }

    private NodeLayout prepareLayout(LayoutContext context, MindmapNode node, int depth, int maxNodeTextWidth) {
        String badgeText = MermaidNodeRenderer.simplifyIcon(node.getIcon());
        String primaryText = node.getText();
        boolean showBadge = badgeText != null && !badgeText.isEmpty()
            && primaryText != null
            && !primaryText.trim()
                .isEmpty()
            && !badgeText.equalsIgnoreCase(primaryText.trim());
        if ((primaryText == null || primaryText.trim()
            .isEmpty()) && badgeText != null) {
            primaryText = badgeText;
            showBadge = false;
            badgeText = null;
        }

        NodeContentLayout contentLayout = prepareNodeContentLayout(context, node, maxNodeTextWidth);
        List<String> lines = new ArrayList<>();
        int textWidth = 0;
        int textHeight = 0;
        if (contentLayout == null) {
            ResolvedTextStyle style = depth == 0 ? ROOT_TEXT_STYLE : NODE_TEXT_STYLE;
            lines = MermaidNodeRenderer.wrapText(context, style, primaryText, maxNodeTextWidth);
            if (lines.isEmpty()) {
                lines.add(" ");
            }

            for (String line : lines) {
                textWidth = Math.max(textWidth, MermaidNodeRenderer.measureText(context, style, line));
            }
            int lineHeight = context.getLineHeight(style);
            textHeight = Math.max(1, lines.size()) * lineHeight;
        } else {
            textWidth = contentLayout.visualBounds()
                .width();
            textHeight = contentLayout.visualBounds()
                .height();
        }

        int badgeWidth = 0;
        int badgeHeight = 0;
        if (showBadge && badgeText != null) {
            badgeWidth = MermaidNodeRenderer.measureText(context, ICON_TEXT_STYLE, badgeText) + 8;
            badgeHeight = context.getLineHeight(ICON_TEXT_STYLE) + 4;
            textWidth = Math.max(textWidth, badgeWidth);
        }

        int width = textWidth + NODE_PADDING_X * 2;
        int height = textHeight + NODE_PADDING_Y * 2;
        if (badgeHeight > 0) {
            height += badgeHeight + ICON_GAP_Y;
        }
        switch (node.getShape()) {
            case ROUNDED -> width += 8;
            case CIRCLE -> {
                width += 12;
                height += 8;
                width = Math.max(width, height + 14);
            }
            case HEXAGON -> width += 14;
            case CLOUD -> width += 16;
            case BANG -> width += 10;
            default -> {}
        }
        if (depth == 0) {
            width += 10;
            height += 4;
        }

        NodeLayout layout = new NodeLayout(node, depth, lines, badgeText, showBadge, contentLayout, width, height);
        for (MindmapNode child : node.getChildren()) {
            layout.children.add(prepareLayout(context, child, depth + 1, maxNodeTextWidth));
        }
        return layout;
    }

    private @Nullable NodeContentLayout prepareNodeContentLayout(LayoutContext context, MindmapNode node,
        int maxNodeTextWidth) {
        LytBlock block = nodeContentBlocks.get(node.getId());
        if (block == null) {
            return null;
        }
        LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
        int contentWidth = Math.clamp(maxNodeTextWidth + 60, 96, 240);
        block.layout(localContext, 0, 0, contentWidth);
        LytRect visualBounds = resolveBlockVisualBounds(block);
        return new NodeContentLayout(block, visualBounds);
    }

    private void measureSideTree(NodeLayout node) {
        if (node.children.isEmpty()) {
            node.subtreeWidth = node.width;
            node.subtreeHeight = node.height;
            return;
        }

        int childrenHeight = 0;
        int childrenWidth = 0;
        for (NodeLayout child : node.children) {
            measureSideTree(child);
            childrenHeight += child.subtreeHeight;
            childrenWidth = Math.max(childrenWidth, child.subtreeWidth);
        }
        childrenHeight += NODE_GAP_Y * (node.children.size() - 1);
        node.subtreeWidth = node.width + NODE_GAP_X + childrenWidth;
        node.subtreeHeight = Math.max(node.height, childrenHeight);
    }

    private void layoutSideTree(NodeLayout node, int anchorX, int centerY, boolean rightSide) {
        if (node.node.getX() != null) {
            node.x = node.node.getX();
        } else {
            node.x = rightSide ? anchorX : anchorX - node.width;
        }
        if (node.node.getY() != null) {
            node.y = node.node.getY();
        } else {
            node.y = centerY - node.height / 2;
        }
        if (node.children.isEmpty()) {
            return;
        }

        int childrenHeight = 0;
        for (NodeLayout child : node.children) {
            childrenHeight += child.subtreeHeight;
        }
        childrenHeight += NODE_GAP_Y * (node.children.size() - 1);

        int cursorY = centerY - childrenHeight / 2;
        for (NodeLayout child : node.children) {
            int childCenterY = cursorY + child.subtreeHeight / 2;
            int childAnchorX = rightSide ? node.x + node.width + NODE_GAP_X : node.x - NODE_GAP_X;
            layoutSideTree(child, childAnchorX, childCenterY, rightSide);
            cursorY += child.subtreeHeight + NODE_GAP_Y;
        }
    }

    private void measureTopDown(NodeLayout node) {
        if (node.children.isEmpty()) {
            node.subtreeWidth = node.width;
            node.subtreeHeight = node.height;
            return;
        }

        int childrenWidth = 0;
        int childrenHeight = 0;
        for (NodeLayout child : node.children) {
            measureTopDown(child);
            childrenWidth += child.subtreeWidth;
            childrenHeight = Math.max(childrenHeight, child.subtreeHeight);
        }
        childrenWidth += NODE_GAP_X * (node.children.size() - 1);
        node.subtreeWidth = Math.max(node.width, childrenWidth);
        node.subtreeHeight = node.height + NODE_GAP_Y + childrenHeight;
    }

    private void layoutTopDown(NodeLayout node, int x, int y) {
        node.x = node.node.getX() != null ? node.node.getX() : x + (node.subtreeWidth - node.width) / 2;
        node.y = node.node.getY() != null ? node.node.getY() : y;
        if (node.children.isEmpty()) {
            return;
        }

        int childrenWidth = 0;
        for (NodeLayout child : node.children) {
            childrenWidth += child.subtreeWidth;
        }
        childrenWidth += NODE_GAP_X * (node.children.size() - 1);

        int cursorX = x + (node.subtreeWidth - childrenWidth) / 2;
        int childY = y + node.height + NODE_GAP_Y;
        for (NodeLayout child : node.children) {
            layoutTopDown(child, cursorX, childY);
            cursorX += child.subtreeWidth + NODE_GAP_X;
        }
    }

    @Override
    @Nullable
    protected NodeHit pickNodeHit(int documentX, int documentY) {
        if (layout == null) {
            return null;
        }
        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();
        List<NodeLayout> contentNodes = layout.contentNodes();
        for (int index = contentNodes.size() - 1; index >= 0; index--) {
            NodeLayout node = contentNodes.get(index);
            LytRect contentScreenRect = getNodeContentScreenRect(node, baseX, baseY, activeZoom);
            if (contentScreenRect == null || !contentScreenRect.contains(documentX, documentY)) {
                continue;
            }
            int localX = unscaleCoordinate(documentX - contentScreenRect.x(), activeZoom);
            int localY = unscaleCoordinate(documentY - contentScreenRect.y(), activeZoom);
            DocumentInteractionSnapshot hit = LytDocument.pick(node.contentLayout.block(), localX, localY);
            if (hit != null) {
                return new NodeHit(hit.node(), hit.flowPath(), localX, localY);
            }
        }
        return null;
    }

    private @Nullable LytRect getNodeContentScreenRect(NodeLayout node, int baseX, int baseY, float activeZoom) {
        if (node.contentLayout == null) {
            return null;
        }
        LytRect nodeRect = new LytRect(
            scaled(baseX, node.x, activeZoom),
            scaled(baseY, node.y, activeZoom),
            Math.max(1, Math.round(node.width * activeZoom)),
            Math.max(1, Math.round(node.height * activeZoom)));
        int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
        int contentY = nodeRect.y() + Math.max(1, Math.round(NODE_PADDING_Y * activeZoom))
            + resolveNodeBadgeHeight(node, activeZoom);
        return resolveNodeContentRect(node.contentLayout, nodeRect, paddingX, contentY, activeZoom);
    }

    private int resolveNodeBadgeHeight(NodeLayout node, float activeZoom) {
        if (!node.showBadge || node.badgeText == null) {
            return 0;
        }
        ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);
        int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
        int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
        return contextLineHeight(badgeStyle) + badgePaddingY * 2 + iconGapY;
    }

    private void restoreViewportAfterLayout(@Nullable DiagramLayout previousLayout, int previousOffsetX,
        int previousOffsetY, int previousViewportWidth, int previousViewportHeight, int viewportWidth,
        int viewportHeight) {
        if (previousLayout == null) {
            centerDiagram(viewportWidth, viewportHeight, layout.diagramWidth(), layout.diagramHeight());
            return;
        }
        float curZoom = getRawZoom();
        float anchorX = previousLayout.contentBounds()
            .x() + (previousViewportWidth * 0.5f - previousOffsetX) / Math.max(curZoom, 0.0001f);
        float anchorY = previousLayout.contentBounds()
            .y() + (previousViewportHeight * 0.5f - previousOffsetY) / Math.max(curZoom, 0.0001f);
        setContentOffset(
            Math.round(
                viewportWidth * 0.5f - (anchorX - layout.contentBounds()
                    .x()) * curZoom),
            Math.round(
                viewportHeight * 0.5f - (anchorY - layout.contentBounds()
                    .y()) * curZoom));
        clampOffsets();
    }

    private LytRect resolveNodeVisualRect(NodeLayout node) {
        LytRect nodeRect = new LytRect(node.x, node.y, node.width, node.height);
        if (node.contentLayout == null) {
            return nodeRect;
        }
        int contentY = node.y + NODE_PADDING_Y + resolveNodeBadgeHeightUnscaled(node);
        LytRect contentRect = new LytRect(
            node.x + NODE_PADDING_X,
            contentY,
            node.contentLayout.visualBounds()
                .width(),
            node.contentLayout.visualBounds()
                .height());
        return LytRect.union(nodeRect, contentRect);
    }

    private int resolveNodeBadgeHeightUnscaled(NodeLayout node) {
        if (!node.showBadge || node.badgeText == null) {
            return 0;
        }
        return contextLineHeight(ICON_TEXT_STYLE) + 4 + ICON_GAP_Y;
    }

    private NodeColors resolveColors(MindmapNode node) {
        int accent = 0xFF7AA2F7;
        for (String className : node.getClasses()) {
            String lower = className.toLowerCase();
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = 0xFFF7768E;
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = 0xFF9ECE6A;
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = 0xFFE0AF68;
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = 0xFF8B949E;
            }
        }

        accent = switch (node.getShape()) {
            case CIRCLE -> 0xFF7DCFFF;
            case HEXAGON -> 0xFFE0AF68;
            case CLOUD -> 0xFF73DACA;
            case BANG -> 0xFFF7768E;
            default -> accent;
        };

        int border = accent;
        int background = node == mindmap.getRoot() ? 0xFF1F2A38 : 0xFF111922;
        return new NodeColors(background, border, accent);
    }

    private int resolvePreferredViewportWidth() {
        return preferredWidth > 0 ? preferredWidth : MIN_WIDTH;
    }

    public static class DiagramLayout {

        private final NodeLayout root;
        private final int diagramWidth;
        private final int diagramHeight;
        private final LytRect contentBounds;
        private final List<NodeLayout> contentNodes;

        public DiagramLayout(NodeLayout root, int diagramWidth, int diagramHeight, LytRect contentBounds,
            List<NodeLayout> contentNodes) {
            this.root = root;
            this.diagramWidth = diagramWidth;
            this.diagramHeight = diagramHeight;
            this.contentBounds = contentBounds;
            this.contentNodes = contentNodes;
        }

        public NodeLayout root() {
            return root;
        }

        public int diagramWidth() {
            return diagramWidth;
        }

        public int diagramHeight() {
            return diagramHeight;
        }

        public LytRect contentBounds() {
            return contentBounds;
        }

        public List<NodeLayout> contentNodes() {
            return contentNodes;
        }
    }

    public static class NodeColors {

        private final int background;
        private final int border;
        private final int accent;

        public NodeColors(int background, int border, int accent) {
            this.background = background;
            this.border = border;
            this.accent = accent;
        }
    }

    public static class NodeLayout {

        private final MindmapNode node;
        private final int depth;
        private final List<String> lines;
        private final String badgeText;
        private final boolean showBadge;
        @Nullable
        private final NodeContentLayout contentLayout;
        private final int width;
        private final int height;
        private final List<NodeLayout> children = new ArrayList<>();

        private int x;
        private int y;
        private int subtreeWidth;
        private int subtreeHeight;

        public NodeLayout(MindmapNode node, int depth, List<String> lines, String badgeText, boolean showBadge,
            @Nullable NodeContentLayout contentLayout, int width, int height) {
            this.node = node;
            this.depth = depth;
            this.lines = lines;
            this.badgeText = badgeText;
            this.showBadge = showBadge;
            this.contentLayout = contentLayout;
            this.width = width;
            this.height = height;
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public int centerX() {
            return x + width / 2;
        }

        public int centerY() {
            return y + height / 2;
        }
    }

    // Debug implementation

    @Override
    public List<ComponentEntry> getDebugComponents() {
        List<ComponentEntry> components = new ArrayList<>();

        if (layout == null || bounds == null) {
            return components;
        }

        LytRect innerViewport = getInnerViewport();
        float activeZoom = getActiveZoom();
        int baseX = innerViewport.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = innerViewport.y() + getVisualOffsetY() - getScaledOriginY();

        // Collect all nodes from the layout
        collectNodeComponents(layout.root(), components, baseX, baseY, activeZoom);

        return components;
    }

    private void collectNodeComponents(NodeLayout node, List<ComponentEntry> components, int baseX, int baseY,
        float activeZoom) {
        if (node == null) {
            return;
        }

        // Calculate node bounds using the same formula as renderNodes
        int nodeScreenX = scaled(baseX, node.x, activeZoom);
        int nodeScreenY = scaled(baseY, node.y, activeZoom);
        int nodeScreenW = Math.max(1, Math.round(node.width * activeZoom));
        int nodeScreenH = Math.max(1, Math.round(node.height * activeZoom));

        LytRect nodeBounds = new LytRect(nodeScreenX, nodeScreenY, nodeScreenW, nodeScreenH);

        // Node info
        String nodeName = String.join(" ", node.lines);
        if (nodeName.length() > 30) {
            nodeName = nodeName.substring(0, 27) + "...";
        }

        String extra = "Depth: " + node.depth;
        if (!node.children.isEmpty()) {
            extra += ", Children: " + node.children.size();
        }

        int priority = 20 - node.depth;
        components.add(new SimpleComponentEntry("Node:" + nodeName, nodeBounds, extra, priority));

        // Add badge as separate component if present
        if (node.showBadge && node.badgeText != null) {
            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
            int badgePaddingX = Math.max(2, Math.round(4 * activeZoom));
            int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
            ResolvedTextStyle badgeStyle = getOrScaleStyle(ICON_TEXT_STYLE, activeZoom);

            // Calculate badge bounds (simplified from renderNodes)
            int badgeWidth = Math.max(1, 100 + badgePaddingX * 2); // Approximate
            int badgeHeight = Math.max(1, 10 + badgePaddingY * 2); // Approximate
            LytRect badgeBounds = new LytRect(nodeScreenX + paddingX, nodeScreenY + paddingY, badgeWidth, badgeHeight);

            components.add(
                new SimpleComponentEntry("Badge:" + node.badgeText, badgeBounds, "Node: " + nodeName, priority + 5));
        }

        // Add node content as separate component if present
        if (node.contentLayout != null) {
            int paddingX = Math.max(1, Math.round(NODE_PADDING_X * activeZoom));
            int paddingY = Math.max(1, Math.round(NODE_PADDING_Y * activeZoom));
            int iconGapY = Math.max(1, Math.round(ICON_GAP_Y * activeZoom));
            int contentY = nodeScreenY + paddingY;

            if (node.showBadge && node.badgeText != null) {
                int badgePaddingY = Math.max(1, Math.round(2 * activeZoom));
                int badgeHeight = Math.max(1, 10 + badgePaddingY * 2); // Approximate
                contentY += badgeHeight + iconGapY;
            }

            LytRect contentRect = resolveNodeContentRect(
                node.contentLayout,
                nodeBounds,
                paddingX,
                contentY,
                activeZoom);
            components.add(
                new SimpleComponentEntry("NodeContent", contentRect, "Block content for: " + nodeName, priority + 3));
        }

        // Recursively collect children
        for (NodeLayout child : node.children) {
            collectNodeComponents(child, components, baseX, baseY, activeZoom);
        }
    }
}
