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

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.internal.debug.DebugFlowContainer;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;
import lombok.Setter;

public abstract class LytMermaidCanvas<T extends LytMermaidCanvas<T>> extends LytBlock
    implements DocumentDragTarget, InteractiveElement {

    private static final float ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;
    static final ConstantColor PANEL_BACKGROUND = new ConstantColor(ColorUtils.ARGB_1A0C1117.getColor());
    static final ConstantColor PANEL_BORDER = new ConstantColor(ColorUtils.ARGB_66434C57.getColor());

    private int contentOffsetX;
    private int contentOffsetY;
    private final SmoothFloatState visualContentOffsetX = new SmoothFloatState();
    private final SmoothFloatState visualContentOffsetY = new SmoothFloatState();
    @Setter
    private float zoom = 1f;
    private final SmoothFloatState visualZoom = new SmoothFloatState();
    private boolean dragging;
    private int dragLastDocumentX;
    private int dragLastDocumentY;

    private final Map<ResolvedTextStyle, ResolvedTextStyle> scaledStyleCache = new IdentityHashMap<>();
    private float lastScaledStyleZoom = Float.NaN;
    @Nullable
    private Object debugComponentLayout;
    @Nullable
    private LytRect debugComponentViewport;
    private int debugComponentOffsetX;
    private int debugComponentOffsetY;
    private float debugComponentZoom;
    private List<DebugComponent.ComponentEntry> cachedDebugComponents = List.of();

    // Common interaction state
    protected Map<String, LytBlock> nodeContentBlocks;
    protected int preferredWidth;
    protected int preferredHeight;
    protected int lastPickDocX;
    protected int lastPickDocY;
    protected boolean lastPickValid;
    @Nullable
    protected LytParagraph lastFlowHoverParagraph;
    @Nullable
    protected LytFlowContent lastFlowHoverContent;

    protected void initNodeContentBlocks(@Nullable Map<String, LytBlock> blocks) {
        this.nodeContentBlocks = blocks == null ? Collections.emptyMap() : new LinkedHashMap<>(blocks);
        for (LytBlock block : this.nodeContentBlocks.values()) {
            block.parent = this;
        }
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return new ArrayList<>(nodeContentBlocks.values());
    }

    protected abstract int canvasPadding();

    protected abstract int contentWidth();

    protected abstract int contentHeight();

    protected abstract int contentOriginX();

    protected abstract int contentOriginY();

    protected abstract boolean diagramReady();

    protected abstract void renderDiagram(RenderContext context, int baseX, int baseY, float activeZoom);

    protected void renderPanel(RenderContext context) {
        context.fillRect(bounds, PANEL_BACKGROUND);
        context.drawBorder(bounds, context.resolveColor(PANEL_BORDER), 1);
    }

    protected void onPreRender() {
        refreshFlowHover();
    }

    @Nullable
    protected abstract NodeHit pickNodeHit(int documentX, int documentY);

    public void setPreferredSize(int width, int height) {
        preferredWidth = Math.max(0, width);
        preferredHeight = Math.max(0, height);
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
        if (!diagramReady() || !getInnerViewport().contains(x, y)) return false;
        NodeHit hit = pickNodeHit(x, y);
        if (hit == null) return false;
        boolean handled = false;
        for (var content : hit.flowPath()
            .targets()) {
            if (content instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
                if (handled) return true;
            }
        }
        for (LytNode current = hit.node(); current != null && current != this
            && !handled; current = current.getParent()) {
            if (current instanceof InteractiveElement interactiveElement) {
                handled = interactiveElement.mouseClicked(screen, hit.localX(), hit.localY(), button, doubleClick);
            }
        }
        return handled;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!diagramReady() || !getInnerViewport().contains((int) x, (int) y)) return Optional.empty();
        NodeHit hit = pickNodeHit((int) x, (int) y);
        if (hit == null) return Optional.empty();
        for (var content : hit.flowPath()
            .targets()) {
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

    protected void refreshFlowHover() {
        if (!lastPickValid || !diagramReady()) return;
        NodeHit hit = pickNodeHit(lastPickDocX, lastPickDocY);
        LytFlowContent hoveredFlow = null;
        LytParagraph hoveredParagraph = null;
        if (hit != null) {
            for (var content : hit.flowPath()
                .targets()) {
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

    public float getActiveZoom() {
        return visualZoom.value();
    }

    public int getVisualOffsetX() {
        return visualContentOffsetX.rounded();
    }

    public int getVisualOffsetY() {
        return visualContentOffsetY.rounded();
    }

    public int getScaledOriginX() {
        return Math.round(contentOriginX() * getActiveZoom());
    }

    public int getScaledOriginY() {
        return Math.round(contentOriginY() * getActiveZoom());
    }

    public LytRect getInnerViewport() {
        return new LytRect(
            bounds.x() + canvasPadding(),
            bounds.y() + canvasPadding(),
            Math.max(1, bounds.width() - canvasPadding() * 2),
            Math.max(1, bounds.height() - canvasPadding() * 2));
    }

    public void updateVisualState() {
        float boundsW = bounds.width();
        float boundsH = bounds.height();
        visualContentOffsetX.updateTowards(contentOffsetX, 26f, 0.05f, 0.01f, Math.max(128f, boundsW * 2f));
        visualContentOffsetY.updateTowards(contentOffsetY, 26f, 0.05f, 0.01f, Math.max(128f, boundsH * 2f));
        visualZoom.updateTowards(zoom, 24f, 0.05f, 0.0001f, 4f);
    }

    public void snapTo(int offsetX, int offsetY, float zoomValue) {
        contentOffsetX = offsetX;
        contentOffsetY = offsetY;
        zoom = zoomValue;
        visualContentOffsetX.snapTo(contentOffsetX);
        visualContentOffsetY.snapTo(contentOffsetY);
        visualZoom.snapTo(zoom);
    }

    public void centerDiagram(int diagramWidth, int diagramHeight) {
        LytRect vp = getInnerViewport();
        snapTo(
            (vp.width() - Math.round(diagramWidth * zoom)) / 2,
            (vp.height() - Math.round(diagramHeight * zoom)) / 2,
            zoom);
    }

    public void centerDiagram(int viewportWidth, int viewportHeight, int diagramWidth, int diagramHeight) {
        int innerWidth = Math.max(1, viewportWidth);
        int innerHeight = Math.max(1, viewportHeight);
        snapTo(
            (innerWidth - Math.round(diagramWidth * zoom)) / 2,
            (innerHeight - Math.round(diagramHeight * zoom)) / 2,
            zoom);
    }

    public void resetView() {
        zoom = 1f;
        centerDiagram(contentWidth(), contentHeight());
        clampOffsets();
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!diagramReady()) return false;
        if (button != 0) return false;
        LytRect vp = getInnerViewport();
        if (!vp.contains(documentX, documentY)) return false;
        dragging = true;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (!dragging) return;
        contentOffsetX += documentX - dragLastDocumentX;
        contentOffsetY += documentY - dragLastDocumentY;
        dragLastDocumentX = documentX;
        dragLastDocumentY = documentY;
        clampOffsets();
    }

    @Override
    public void endDrag() {
        dragging = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (!diagramReady()) return false;
        if (wheelDelta == 0) return false;
        LytRect vp = getInnerViewport();
        if (!vp.contains(documentX, documentY)) return false;

        int previousOffsetX = contentOffsetX;
        int previousOffsetY = contentOffsetY;
        float previousZoom = zoom;

        zoom = wheelDelta > 0 ? Math.min(MAX_ZOOM, zoom * ZOOM_STEP) : Math.max(MIN_ZOOM, zoom / ZOOM_STEP);

        if (Math.abs(previousZoom - zoom) < 0.0001f) return false;

        float anchorX = contentOriginX() + (documentX - vp.x() - previousOffsetX) / Math.max(previousZoom, 0.0001f);
        float anchorY = contentOriginY() + (documentY - vp.y() - previousOffsetY) / Math.max(previousZoom, 0.0001f);
        contentOffsetX = Math.round((documentX - vp.x()) - (anchorX - contentOriginX()) * zoom);
        contentOffsetY = Math.round((documentY - vp.y()) - (anchorY - contentOriginY()) * zoom);
        clampOffsets();
        return true;
    }

    public void setContentOffset(int x, int y) {
        contentOffsetX = x;
        contentOffsetY = y;
    }

    public int getRawOffsetX() {
        return contentOffsetX;
    }

    public int getRawOffsetY() {
        return contentOffsetY;
    }

    public float getRawZoom() {
        return zoom;
    }

    public void clampOffsets() {
        int innerWidth = Math.max(1, bounds.width() - canvasPadding() * 2);
        int innerHeight = Math.max(1, bounds.height() - canvasPadding() * 2);
        contentOffsetX = clampAxis(contentOffsetX, innerWidth, Math.round(contentWidth() * zoom));
        contentOffsetY = clampAxis(contentOffsetY, innerHeight, Math.round(contentHeight() * zoom));
    }

    @Override
    public void render(RenderContext context) {
        if (!diagramReady()) return;
        onPreRender();
        updateVisualState();

        float activeZoom = getActiveZoom();
        if (Float.compare(lastScaledStyleZoom, activeZoom) != 0) {
            scaledStyleCache.clear();
            lastScaledStyleZoom = activeZoom;
        }

        renderPanel(context);

        LytRect inner = getInnerViewport();
        int baseX = inner.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = inner.y() + getVisualOffsetY() - getScaledOriginY();

        context.pushLocalScissor(inner);
        try {
            renderDiagram(context, baseX, baseY, activeZoom);
        } finally {
            context.popScissor();
        }
    }

    protected ResolvedTextStyle getOrScaleStyle(ResolvedTextStyle base, float zoom) {
        return MermaidNodeRenderer.getOrScaleStyle(scaledStyleCache, base, zoom);
    }

    @Nullable
    protected List<DebugComponent.ComponentEntry> getCachedDebugComponents(Object layout) {
        LytRect viewport = getInnerViewport();
        int offsetX = getVisualOffsetX();
        int offsetY = getVisualOffsetY();
        float activeZoom = getActiveZoom();
        if (layout != debugComponentLayout || !viewport.equals(debugComponentViewport)
            || offsetX != debugComponentOffsetX
            || offsetY != debugComponentOffsetY
            || Float.compare(activeZoom, debugComponentZoom) != 0) {
            return null;
        }
        return cachedDebugComponents;
    }

    protected List<DebugComponent.ComponentEntry> cacheDebugComponents(Object layout,
        List<DebugComponent.ComponentEntry> components) {
        debugComponentLayout = layout;
        debugComponentViewport = getInnerViewport();
        debugComponentOffsetX = getVisualOffsetX();
        debugComponentOffsetY = getVisualOffsetY();
        debugComponentZoom = getActiveZoom();
        cachedDebugComponents = List.copyOf(components);
        return cachedDebugComponents;
    }

    public static int clampAxis(int offset, int viewportSize, int contentSize) {
        int minimumVisible = Math.max(1, Math.min(contentSize, viewportSize) / 2);
        int minOffset = minimumVisible - contentSize;
        int maxOffset = viewportSize - minimumVisible;
        return Math.clamp(offset, minOffset, maxOffset);
    }

    public static int scaled(int base, int value, float activeZoom) {
        return base + Math.round(value * activeZoom);
    }

    protected static boolean usesRawGl(LytBlock block) {
        return block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock
            || block instanceof LytItemImage
            || block instanceof LytNeiRecipeBox;
    }

    protected static void renderContainerDecoration(LytNode container, RenderContext context) {
        if (!(container instanceof LytBox box)) return;
        LytRect b = container.getBounds();
        if (box.getBackgroundColor() != null) {
            context.fillRect(b, box.getBackgroundColor());
        }
        int topW = box.getBorderTop()
            .width();
        int bottomW = box.getBorderBottom()
            .width();
        if (topW > 0) {
            context.fillRect(
                b.x(),
                b.y(),
                b.width(),
                topW,
                context.resolveColor(
                    box.getBorderTop()
                        .color()));
        }
        if (bottomW > 0) {
            context.fillRect(
                b.x(),
                b.bottom() - bottomW,
                b.width(),
                bottomW,
                context.resolveColor(
                    box.getBorderBottom()
                        .color()));
        }
    }

    protected static LytRect resolveBlockVisualBounds(LytBlock block) {
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
        if (bounds == null) {
            return LytRect.empty();
        }
        if (block instanceof LytLatexBlock latexBlock) {
            return latexBlock.getVisualBounds();
        }
        if (block instanceof LytLatexDisplayBlock latexDisplayBlock) {
            return latexDisplayBlock.getVisualBounds();
        }
        return bounds;
    }

    @Nullable
    protected static LytRect intersect(LytRect a, LytRect b) {
        int left = Math.max(a.x(), b.x());
        int top = Math.max(a.y(), b.y());
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (right <= left || bottom <= top) {
            return null;
        }
        return new LytRect(left, top, right - left, bottom - top);
    }

    protected static int unscaleCoordinate(int coordinate, float activeZoom) {
        return Math.max(0, Math.round(coordinate / Math.max(activeZoom, 0.0001f)));
    }

    protected static int contextLineHeight(ResolvedTextStyle style) {
        return Math.max(1, Math.round((9 + 1) * style.fontScale()));
    }

    protected void renderNodeContentBlock(LytBlock block, NodeContentRenderContext nodeContext) {
        if (block instanceof LytNode container && !container.getChildren()
            .isEmpty()) {
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

    protected final void renderNodeContent(RenderContext context, LytBlock block, LytRect contentViewport,
        LytRect visualBounds, float activeZoom) {
        LytRect innerViewport = getInnerViewport();
        LytRect clip = intersect(innerViewport, contentViewport);
        if (clip == null) return;
        context.pushLocalScissor(clip);
        try {
            int originX = contentViewport.x() - Math.round(visualBounds.x() * activeZoom);
            int originY = contentViewport.y() - Math.round(visualBounds.y() * activeZoom);
            NodeContentRenderContext nodeContext = new NodeContentRenderContext(
                context,
                clip,
                originX,
                originY,
                activeZoom);
            renderNodeContentBlock(block, nodeContext);
        } finally {
            context.popScissor();
        }
    }

    protected static LytRect resolveNodeContentRect(NodeContentLayout contentLayout, LytRect nodeRect, int paddingX,
        int contentY, float activeZoom) {
        return new LytRect(
            nodeRect.x() + paddingX,
            contentY,
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
    }

    protected void collectNodeContentDebugComponents(NodeContentLayout contentLayout, LytRect contentViewport,
        float activeZoom, String nodeName, int priority, List<DebugComponent.ComponentEntry> components) {
        if (contentLayout == null || contentViewport == null
            || contentLayout.visualBounds()
                .isEmpty()) {
            return;
        }
        int originX = contentViewport.x() - Math.round(
            contentLayout.visualBounds()
                .x() * activeZoom);
        int originY = contentViewport.y() - Math.round(
            contentLayout.visualBounds()
                .y() * activeZoom);
        collectNodeContentDebugComponents(
            contentLayout.block(),
            originX,
            originY,
            activeZoom,
            nodeName,
            priority,
            components,
            0);
    }

    private void collectNodeContentDebugComponents(LytNode node, int originX, int originY, float activeZoom,
        String nodeName, int priority, List<DebugComponent.ComponentEntry> components, int depth) {
        LytRect localBounds = node.getBounds();
        if (localBounds != null && !localBounds.isEmpty()) {
            LytRect screenBounds = new LytRect(
                originX + Math.round(localBounds.x() * activeZoom),
                originY + Math.round(localBounds.y() * activeZoom),
                Math.max(1, Math.round(localBounds.width() * activeZoom)),
                Math.max(1, Math.round(localBounds.height() * activeZoom)));
            components.add(
                new DebugComponent.SimpleComponentEntry(
                    "NodeContent:" + nodeName
                        + ":"
                        + node.getClass()
                            .getSimpleName(),
                    screenBounds,
                    null,
                    priority + depth));
            if (node instanceof DebugFlowContainer flowContainer) {
                for (DebugFlowContainer.FlowContentEntry entry : flowContainer.getAllFlowContent()) {
                    LytRect flowBounds = entry.bounds();
                    components.add(
                        new DebugComponent.SimpleComponentEntry(
                            "NodeContent:" + nodeName
                                + ":"
                                + entry.content()
                                    .getClass()
                                    .getSimpleName(),
                            new LytRect(
                                originX + Math.round(flowBounds.x() * activeZoom),
                                originY + Math.round(flowBounds.y() * activeZoom),
                                Math.max(1, Math.round(flowBounds.width() * activeZoom)),
                                Math.max(1, Math.round(flowBounds.height() * activeZoom))),
                            null,
                            priority + depth + 10));
                }
            }
        }
        for (LytNode child : node.getChildren()) {
            collectNodeContentDebugComponents(
                child,
                originX,
                originY,
                activeZoom,
                nodeName,
                priority,
                components,
                depth + 1);
        }
    }

    public record NodeHit(LytNode node, FlowInteractionPath flowPath, int localX, int localY) {

        public NodeHit(LytNode node, @Nullable FlowInteractionPath flowPath, int localX, int localY) {
            this.node = node;
            this.flowPath = flowPath != null ? flowPath : FlowInteractionPath.empty();
            this.localX = localX;
            this.localY = localY;
        }
    }

    public record NodeContentLayout(LytBlock block, LytRect visualBounds) {

        public NodeContentLayout(LytBlock block, LytRect visualBounds) {
            this.block = block;
            this.visualBounds = visualBounds != null && !visualBounds.isEmpty() ? visualBounds : LytRect.empty();
        }
    }

    public static class NodeContentRenderContext implements RenderContext {

        private final RenderContext delegate;
        private final LytRect viewport;
        private final int originX;
        private final int originY;
        @Getter
        private final float scale;
        private final Map<ResolvedTextStyle, ResolvedTextStyle> scaledStyleCache = new IdentityHashMap<>();

        public NodeContentRenderContext(RenderContext delegate, LytRect viewport, int originX, int originY,
            float scale) {
            this.delegate = delegate;
            this.viewport = new LytRect(
                0,
                0,
                Math.max(1, Math.round(viewport.width() / scale)),
                Math.max(1, Math.round(viewport.height() / scale)));
            this.originX = originX;
            this.originY = originY;
            this.scale = Math.max(0.0001f, scale);
        }

        @Override
        public LytRect viewport() {
            return viewport;
        }

        @Override
        public int getDocumentOriginX() {
            return originX;
        }

        @Override
        public int getDocumentOriginY() {
            return originY;
        }

        @Override
        public LytRect toScreenRect(LytRect rect) {
            LytRect s = scaleRect(rect);
            return new LytRect(
                s.x() + delegate.getDocumentOriginX(),
                s.y() + delegate.getDocumentOriginY() - delegate.getScrollOffsetY(),
                s.width(),
                s.height());
        }

        @Override
        public int resolveColor(ColorValue ref) {
            return delegate.resolveColor(ref);
        }

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
            delegate.drawBorder(
                scaleX(x),
                scaleY(y),
                scaleLength(width),
                scaleLength(height),
                argbColor,
                Math.max(1, scaleLength(thickness)));
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
                if (overlay) {
                    delegate.renderItem(stack, 0, 0);
                } else {
                    delegate.renderItemIcon(stack, 0, 0);
                }
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
                    sprite.getTexture(),
                    0,
                    0,
                    sprite.getU(),
                    sprite.getV(),
                    sprite.getWidth(),
                    sprite.getHeight());
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
            delegate.drawLine(
                scaleFloatX(x1),
                scaleFloatY(y1),
                scaleFloatX(x2),
                scaleFloatY(y2),
                Math.max(1f, thickness * scale),
                argbColor);
        }

        @Override
        public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
            delegate.fillTriangle(
                scaleFloatX(x1),
                scaleFloatY(y1),
                scaleFloatX(x2),
                scaleFloatY(y2),
                scaleFloatX(x3),
                scaleFloatY(y3),
                argbColor);
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
        public void fillEllipse(float cx, float cy, float rx, float ry, int argbColor) {
            delegate.fillEllipse(scaleFloatX(cx), scaleFloatY(cy), rx * scale, ry * scale, argbColor);
        }

        @Override
        public void drawCircleOutline(float cx, float cy, float radius, float thickness, int argbColor) {
            delegate.drawCircleOutline(
                scaleFloatX(cx),
                scaleFloatY(cy),
                radius * scale,
                Math.max(1f, thickness * scale),
                argbColor);
        }

        @Override
        public void pushScissor(LytRect rect) {
            delegate.pushScissor(scaleRect(rect));
        }

        @Override
        public void pushLocalScissor(LytRect rect) {
            delegate.pushLocalScissor(scaleRect(rect));
        }

        @Override
        public LytRect currentScissor() {
            return delegate.currentScissor();
        }

        @Override
        public void popScissor() {
            delegate.popScissor();
        }

        @Override
        public void restoreExternalRenderState() {
            delegate.restoreExternalRenderState();
        }

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
            return scaledStyleCache.computeIfAbsent(
                style,
                key -> new ResolvedTextStyle(
                    key.fontScale() * scale,
                    key.bold(),
                    key.italic(),
                    key.underlined(),
                    key.wavyUnderline(),
                    key.dottedUnderline(),
                    key.strikethrough(),
                    key.obfuscated(),
                    key.font(),
                    key.color(),
                    key.whiteSpace(),
                    key.alignment(),
                    key.dropShadow(),
                    key.backgroundColor(),
                    key.inlineCode()));
        }

        private LytRect scaleRect(LytRect rect) {
            return new LytRect(
                scaleX(rect.x()),
                scaleY(rect.y()),
                scaleLength(rect.width()),
                scaleLength(rect.height()));
        }

        private int scaleX(int x) {
            return originX + Math.round(x * scale);
        }

        private int scaleY(int y) {
            return originY + Math.round(y * scale);
        }

        private int scaleLength(int value) {
            return Math.max(1, Math.round(value * scale));
        }

        private float scaleFloatX(float x) {
            return originX + x * scale;
        }

        private float scaleFloatY(float y) {
            return originY + y * scale;
        }
    }
}
