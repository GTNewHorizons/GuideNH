package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Setter;

public abstract class LytMermaidCanvas<T extends LytMermaidCanvas<T>> extends LytBlock
    implements DocumentDragTarget, InteractiveElement {

    private static final float ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;
    static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);

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

    // Common interaction state
    protected Map<String, LytBlock> nodeContentBlocks;
    protected int preferredWidth;
    protected int preferredHeight;

    protected void initNodeContentBlocks(@Nullable Map<String, LytBlock> blocks) {
        this.nodeContentBlocks = blocks == null ? Collections.emptyMap() : new LinkedHashMap<>(blocks);
        for (LytBlock block : this.nodeContentBlocks.values()) {
            block.parent = this;
        }
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of();
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        if (includeOutOfTreeContent && nodeContentBlocks != null) {
            for (LytBlock block : nodeContentBlocks.values()) {
                if (block.visit(visitor, true) == LytVisitor.Result.STOP) {
                    return LytVisitor.Result.STOP;
                }
            }
        }
        return LytVisitor.Result.CONTINUE;
    }

    @Override
    public int getExplicitWidth() {
        return preferredWidth > 0 ? preferredWidth : -1;
    }

    @Override
    public int getExplicitHeight() {
        return preferredHeight > 0 ? preferredHeight : -1;
    }

    protected abstract int canvasPadding();

    protected abstract int contentWidth();

    protected abstract int contentHeight();

    protected abstract int contentOriginX();

    protected abstract int contentOriginY();

    protected abstract boolean diagramReady();

    protected void renderPanel(RenderContext context) {
        context.fillRect(bounds, PANEL_BACKGROUND);
        context.drawBorder(bounds, context.resolveColor(PANEL_BORDER), 1);
    }

    @Override
    public void render(RenderContext context) {
        // Unused: subclasses use the primitives path (usePrimitives() == true).
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
    public void computePrimitives(PrimitiveCollector c) {
        if (!diagramReady()) return;
        LytRect b = getBounds();
        if (b == null) return;

        // Panel background and border
        c.emit(
            new GuideRenderPrimitive.FillRect(
                b.x(),
                b.y(),
                b.width(),
                b.height(),
                PANEL_BACKGROUND.resolve(LightDarkMode.current())));
        c.emit(
            new GuideRenderPrimitive.DrawBorder(
                b.x(),
                b.y(),
                b.width(),
                b.height(),
                1,
                1,
                1,
                1,
                PANEL_BORDER.resolve(LightDarkMode.current())));

        float activeZoom = getActiveZoom();
        LytRect inner = getInnerViewport();
        int baseX = inner.x() + getVisualOffsetX() - getScaledOriginX();
        int baseY = inner.y() + getVisualOffsetY() - getScaledOriginY();

        emitDiagramPrimitives(c, baseX, baseY, activeZoom);
    }

    /**
     * Subclasses override to emit diagram-specific primitives (edges, nodes,
     * content blocks) after the panel has been emitted.
     */
    protected void emitDiagramPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {}

    protected ResolvedTextStyle getOrScaleStyle(ResolvedTextStyle base, float zoom) {
        return MermaidNodeRenderer.getOrScaleStyle(scaledStyleCache, base, zoom);
    }

    public static int clampAxis(int offset, int viewportSize, int contentSize) {
        if (contentSize <= viewportSize) {
            return (viewportSize - contentSize) / 2;
        }
        return Math.clamp(offset, viewportSize - contentSize, 0);
    }

    public static int scaled(int base, int value, float activeZoom) {
        return base + Math.round(value * activeZoom);
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

    // ---- primitives-path helpers for node content blocks ----

    /**
     * Emit primitives for a node content block using the collector, replacing
     * the legacy NodeContentRenderContext path. The block is rendered inside
     * a PushTransform/PopTransform frame so its local coordinates map to the
     * correct screen position.
     */
    protected void emitNodeContentPrimitives(PrimitiveCollector c, LytBlock block, LytRect contentViewport,
        LytRect visualBounds, float activeZoom) {
        LytRect innerViewport = getInnerViewport();
        LytRect clip = intersect(innerViewport, contentViewport);
        if (clip == null) return;
        int originX = contentViewport.x() - Math.round(visualBounds.x() * activeZoom);
        int originY = contentViewport.y() - Math.round(visualBounds.y() * activeZoom);
        c.pushScissor(clip.x(), clip.y(), clip.width(), clip.height());
        c.pushTransform(originX, originY, activeZoom);
        c.collectFrom(block);
        c.popTransform();
        c.popScissor();
    }

    /**
     * Overload that prepares the content viewport from a NodeContentLayout
     * and a screen-space content area, then delegates to the 5-arg variant.
     */
    protected void emitNodeContentPrimitives(PrimitiveCollector c, NodeContentLayout contentLayout,
        LytRect contentArea, float activeZoom) {
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
        LytRect contentViewport = new LytRect(cvpX, cvpY, rawViewport.width(), rawViewport.height());
        emitNodeContentPrimitives(c, contentLayout.block(), contentViewport, contentLayout.visualBounds(), activeZoom);
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

}
