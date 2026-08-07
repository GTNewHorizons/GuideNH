package com.hfstudio.guidenh.guide.document.block;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.LayoutTreeSerializer;
import com.hfstudio.guidenh.guide.layout.Layouts;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.render.GlyphRunData;
import com.hfstudio.guidenh.guide.render.GlyphRunGroup;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Setter;

public abstract class LytMermaidCanvas<T extends LytMermaidCanvas<T>> extends LytBlock
    implements DocumentDragTarget, InteractiveElement {

    private static final boolean HEADLESS = Boolean.getBoolean("guidenh.headlessRender");

    private static final float ZOOM_STEP = 1.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 5.0f;
    static final ConstantColor PANEL_BACKGROUND = new ConstantColor(0x1A0C1117);
    static final ConstantColor PANEL_BORDER = new ConstantColor(0x66434C57);

    /**
     * Rust layout engine's document content-box padding (layout-engine
     * {@code CONTENT_PAD}, layout.rs:17). The engine insets every serialized
     * tree by this amount on all sides. A standalone NodeContent subtree
     * serialized through {@link #layoutNodeContentWithRust} must therefore
     * inflate its requested available width by 2×PAD so the inner content
     * width equals {@code contentWidth} — keeping node sizing identical to the
     * pre-Rust Java path (LytParagraph used to claim the full availableWidth).
     */
    private static final int RUST_CONTENT_PAD = 14;

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

    /**
     * Headless render injection, applied by RenderPageService from
     * {@code -Dguidenh.renderpage.mermaidzoom} / {@code -Dguidenh.renderpage.mermaidoffset}
     * (mirroring the navscroll injection pattern). Zero zoom and zero offsets
     * mean "no injection": the {@code HEADLESS} branch then keeps the
     * historical fit-to-view + centre behaviour byte-identical.
     */
    private float headlessZoomInjection;
    private int headlessOffsetXInjection;
    private int headlessOffsetYInjection;

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

    /**
     * Active zoom used for rendering.
     * <p>
     * <b>Upper ceiling:</b> every path — interactive scroll, direct-write
     * {@code snapTo}, and the headless injection branch — is bounded by
     * {@link #MAX_ZOOM} on every read. The ceiling guards glyph rasterization:
     * an unclamped huge zoom would push fontScale to enormous sizes and
     * overflow the glyph atlas pages.
     * <p>
     * <b>Lower floor:</b> interactive zoom and headless zoom injection are
     * floored at {@link #MIN_ZOOM}. The headless <em>fit-to-view</em> zoom
     * (no injection) is exempt from the floor: it must stay at its exact
     * computed value so a diagram larger than the viewport always fits
     * (byte-identical no-injection regression). It is always {@code <= 1.0}
     * by construction, so only the defensive upper bound applies.
     * <p>
     * <b>Tier quantization:</b> the interactive and headless-injection paths
     * snap their value to the nearest {@link #ZOOM_STEP}^n tier (scroll steps
     * are themselves powers of {@link #ZOOM_STEP}, so targets are natively
     * near-tier). Quantizing the easing intermediate values keeps the fontScale
     * — and therefore the GuideText shape cache key — stable during a zoom
     * animation instead of changing every frame (the per-frame re-rasterize /
     * per-glyph re-upload churn that collapsed the frame rate). The
     * no-injection fit path is exempt: fitZoom is always {@code <= 1.0} and is
     * returned unquantized (byte-identical no-injection regression).
     */
    public float getActiveZoom() {
        if (HEADLESS) {
            if (headlessZoomInjection > 0f) {
                return quantizeZoom(Math.clamp(zoom, MIN_ZOOM, MAX_ZOOM));
            }
            // No-injection fit-to-view: exact value, never quantized.
            return Math.min(MAX_ZOOM, zoom);
        }
        float v = visualZoom.value();
        return quantizeZoom(Math.clamp(v > 0f ? v : zoom, MIN_ZOOM, MAX_ZOOM));
    }

    /**
     * Quantize a zoom value to the nearest {@code ZOOM_STEP^n} tier (n an
     * integer), then clamp the tier back into {@code [MIN_ZOOM, MAX_ZOOM]}.
     * <p>
     * Order is semantically: clamp input, quantize, clamp tier. The final
     * clamp guarantees the {@link #MAX_ZOOM} ceiling that guards glyph
     * rasterization is never exceeded even when the nearest tier above
     * {@code MAX_ZOOM} (1.1^17 ≈ 5.0545) would overshoot it — the returned
     * value is always a 1.1^n tier except exactly at the [MIN, MAX] bounds.
     */
    private static float quantizeZoom(float value) {
        if (value <= 0f) {
            return value;
        }
        double tier = Math.pow(ZOOM_STEP, Math.round(Math.log(value) / Math.log(ZOOM_STEP)));
        return (float) Math.clamp(tier, MIN_ZOOM, MAX_ZOOM);
    }

    public int getVisualOffsetX() {
        return HEADLESS ? contentOffsetX : visualContentOffsetX.rounded();
    }

    public int getVisualOffsetY() {
        return HEADLESS ? contentOffsetY : visualContentOffsetY.rounded();
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

    /**
     * Apply headless zoom/offset injection. Zero zoom and zero offsets leave
     * the {@code HEADLESS} branch on its historical fit-to-view + centre path.
     */
    public void setHeadlessInjection(float zoomInjection, int offsetX, int offsetY) {
        this.headlessZoomInjection = zoomInjection;
        this.headlessOffsetXInjection = offsetX;
        this.headlessOffsetYInjection = offsetY;
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
        // Drive the raw→visual easing chain at the entry of our own primitive
        // collection (same pattern as LytCodeBlock driving updateVisualScroll in
        // its computePrimitives): wheel-zoom (scroll) and drags write raw zoom /
        // contentOffset, and getActiveZoom/getVisualOffsetX/Y read the visual
        // side — without a per-frame driver the two stay permanently detached
        // and the interaction never reaches the render. HEADLESS mode reads the
        // raw values directly and the HEADLESS branch below overrides zoom /
        // offset anyway, so this call is a no-op for headless rendering.
        updateVisualState();
        boolean ready = diagramReady();
        GuideDebugLog.debugAlways("[GuideNH-Mermaid] computePrimitives diagramReady={} bounds={}",
            ready, bounds);
        if (!ready) return;
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
        int offsetX = getVisualOffsetX();
        int offsetY = getVisualOffsetY();
        if (HEADLESS) {
            // Headless: fit diagram in viewport with fit-to-view zoom, unless
            // a -Dguidenh.renderpage.mermaidzoom / mermaidoffset injection was
            // applied via setHeadlessInjection. Without injection the
            // historical fit-to-view + centre behaviour is preserved
            // byte-identically.
            int contentW = contentWidth();
            int contentH = contentHeight();
            if (contentW > 0 && contentH > 0) {
                float fitZoom = Math.min(1f, Math.min(
                    (float) inner.width() / contentW,
                    (float) inner.height() / contentH));
                zoom = headlessZoomInjection > 0f ? headlessZoomInjection : fitZoom;
                // Route the injected zoom through getActiveZoom so it shares the
                // [MIN_ZOOM, MAX_ZOOM] clamp (an over-ceiling -D injection is
                // clamped to MAX_ZOOM instead of overflowing the atlas pages).
                // The no-injection fit-to-view value is preserved exactly (the
                // diagram must always fit; byte-identical regression).
                activeZoom = getActiveZoom();
                if (headlessZoomInjection > 0f) {
                    GuideDebugLog.infoAlways(
                        "[GuideNH-Mermaid] headless zoom injection: requested={} quantized={}",
                        zoom, activeZoom);
                }
            }
            int scaledContentW = Math.round(contentWidth() * activeZoom);
            int scaledContentH = Math.round(contentHeight() * activeZoom);
            if (headlessOffsetXInjection != 0 || headlessOffsetYInjection != 0) {
                offsetX = headlessOffsetXInjection;
                offsetY = headlessOffsetYInjection;
            } else {
                offsetX = (inner.width() - scaledContentW) / 2;
                offsetY = (inner.height() - scaledContentH) / 2;
            }
        }
        int baseX = inner.x() + offsetX - getScaledOriginX();
        int baseY = inner.y() + offsetY - getScaledOriginY();

        // Clip diagram primitives to the inner viewport (prevent overflow to
        // subsequent page content).
        c.pushScissor(inner.x(), inner.y(), inner.width(), inner.height());
        emitDiagramPrimitives(c, baseX, baseY, activeZoom);
        c.popScissor();
    }

    /**
     * Subclasses override to emit diagram-specific primitives (edges, nodes,
     * content blocks) after the panel has been emitted.
     */
    protected void emitDiagramPrimitives(PrimitiveCollector c, int baseX, int baseY, float activeZoom) {}

    protected ResolvedTextStyle getOrScaleStyle(ResolvedTextStyle base, float zoom) {
        // The scaled-style cache is keyed by the base style only (not by zoom),
        // so it must be invalidated whenever the zoom changes — otherwise the
        // text fontScale would keep the stale zoom and text would stop scaling
        // in sync with the node boxes. Clearing on zoom change rebuilds the
        // cache for the current zoom (cheap: at most a handful of base styles).
        if (Float.compare(zoom, lastScaledStyleZoom) != 0) {
            lastScaledStyleZoom = zoom;
            scaledStyleCache.clear();
        }
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
        int availW = Math.max(1, nodeRect.width() - paddingX * 2);
        int availH = Math.max(1, nodeRect.y() + nodeRect.height() - contentY);
        return new LytRect(
            nodeRect.x() + paddingX,
            contentY,
            Math.min(
                Math.max(1, Math.round(contentLayout.visualBounds().width() * activeZoom)),
                availW),
            Math.min(
                Math.max(1, Math.round(contentLayout.visualBounds().height() * activeZoom)),
                availH));
    }

    // ---- primitives-path helpers for node content blocks ----

    /**
     * Lay out a Mermaid NodeContent root block through the Rust layout engine
     * — the same serialize → measureLayout → writeback pipeline the main
     * document uses ({@code LytDocument.createLayout}). This is required
     * because NodeContent subtrees live off the document tree
     * ({@code nodeContentBlocks}; {@link #getChildren()} is empty), so they
     * never reach the document's Rust pass and their inline blocks keep a zero
     * x-position (LytItemImage draws at {@code bounds.x()} → line start). A
     * dedicated {@link LayoutTreeSerializer} + {@link LayoutBridge#measureLayout}
     * pass runs Rust's inline post-pass on the subtree, which anchors each
     * inline block at its paragraph marker's pen position and writes the real x
     * back into its bounds.
     * <p>
     * <b>Coordinate system:</b> the FlatLayout/glyph coordinates come back
     * <b>relative to the subtree's serialized root</b> (the root sits at the
     * engine's {@code CONTENT_PAD} inset — i.e. (14,14) for this subtree). Both
     * {@link #resolveBlockVisualBounds} and {@link #emitNodeContentPrimitives}
     * consume that same shifted space (the viewport origin subtracts
     * {@code visualBounds.x()/y()} while the block/glyph coordinates include
     * the identical inset), so the existing viewport translation stays valid
     * without any extra offset math.
     * <p>
     * Falls back to the Java manual layout ({@link #layoutContentSubtree}) when
     * the native bridge is unavailable (font handle 0) or the measure pass
     * fails, so NodeContent stays visible in environments without a loaded
     * layout engine. Paragraph glyph runs are wiped before the pass so a failed
     * pass never renders stale runs at outdated coordinates.
     *
     * @param context      layout context (font metrics + visual scale)
     * @param block        the NodeContent root block (usually the LytVBox
     *                     produced by {@code compileNodeContentBlock})
     * @param contentWidth the content width used to lay the block out
     */
    protected void layoutNodeContentWithRust(LayoutContext context, LytBlock block, int contentWidth) {
        LayoutContext localContext = new LayoutContext(context).withVisualScale(context.getVisualScale());
        // Root's own Java bounds are meaningless (LytVBox.computeBoxLayout is a
        // stub), but keep the call so the Java fallback sees the same
        // preconditions as before.
        block.layout(localContext, 0, 0, contentWidth);
        clearGlyphRuns(block);
        long fontHandle = LayoutBridge.getFontHandle();
        if (fontHandle != 0) {
            try {
                var serializer = new LayoutTreeSerializer();
                byte[] input = serializer.serialize(
                    block,
                    contentWidth + 2 * RUST_CONTENT_PAD,
                    localContext.getVisualScale(),
                    DisplayScale.scaleFactor());
                byte[] result = LayoutBridge.measureLayout(fontHandle, input);
                if (result.length > 0) {
                    var flatResult = LayoutResult.getRootAsLayoutResult(ByteBuffer.wrap(result));
                    // Upload unique glyph bitmaps to the shared atlas (keys are
                    // content-stable, so repeated uploads are no-ops).
                    var atlas = GuideGlyphAtlas.instance();
                    int numBitmaps = flatResult.bitmapsLength();
                    for (int bi = 0; bi < numBitmaps; bi++) {
                        var bmp = flatResult.bitmaps(bi);
                        if (bmp == null) continue;
                        int w = (int) bmp.w();
                        int h = (int) bmp.h();
                        if (w <= 0 || h <= 0) continue;
                        ByteBuffer rgbaBuf = bmp.rgbaAsByteBuffer();
                        byte[] rgba = new byte[rgbaBuf.remaining()];
                        rgbaBuf.get(rgba);
                        if (w > 200 || h > 200) {
                            GuideDebugLog.warnAlways(
                                "[GuideNH] OVERSIZE glyph upload source=LytMermaidCanvas key={} w={} h={}",
                                bmp.key(), w, h);
                        }
                        atlas.upload(bmp.key(), rgba, w, h);
                    }
                    // Writeback: every serialized subtree block gets its
                    // Rust-computed bounds (glyph runs and inline blocks
                    // included). The vector is index-aligned with the
                    // serializer's flat nodes.
                    int numLayouts = flatResult.nodesLength();
                    for (int i = 0; i < numLayouts; i++) {
                        var fl = flatResult.nodes(i);
                        if (fl == null) continue;
                        LytNode node = serializer.getNodeByFlatIndex(i);
                        if (!(node instanceof LytBlock lb)) continue;
                        lb.applyExternalLayout(new LytRect(
                            Math.round(fl.x()),
                            Math.round(fl.y()),
                            Math.max(0, Math.round(fl.w())),
                            Math.max(0, Math.round(fl.h()))));
                    }
                    // Inject glyph runs (final subtree-space quads) and span
                    // decoration rects into the paragraphs so they render rich
                    // text exactly like the main document pipeline.
                    Map<Integer, List<GlyphRunGroup>> runsByNode = new HashMap<>();
                    int numRuns = flatResult.glyphRunsLength();
                    for (int ri = 0; ri < numRuns; ri++) {
                        var fbRun = flatResult.glyphRuns(ri);
                        if (fbRun == null) continue;
                        int numGlyphs = fbRun.glyphsLength();
                        var placed = new ArrayList<GuideRenderPrimitive.PlacedGlyph>(numGlyphs);
                        for (int gi = 0; gi < numGlyphs; gi++) {
                            var fbg = fbRun.glyphs(gi);
                            if (fbg != null) {
                                placed.add(new GuideRenderPrimitive.PlacedGlyph(
                                    fbg.bitmapKey(),
                                    fbg.x(),
                                    fbg.y(),
                                    fbg.w(),
                                    fbg.h(),
                                    (int) fbg.lineIndex()));
                            }
                        }
                        runsByNode.computeIfAbsent((int) fbRun.nodeIndex(), k -> new ArrayList<>())
                            .add(new GlyphRunGroup(placed, (int) fbRun.argb(), fbRun.shear()));
                    }
                    Map<Integer, List<GuideRenderPrimitive.FillRect>> backgroundsByNode = new HashMap<>();
                    Map<Integer, List<GuideRenderPrimitive.FillRect>> linesByNode = new HashMap<>();
                    Map<Integer, List<GuideRenderPrimitive.FillRect>> separatorsByNode = new HashMap<>();
                    int numDecorations = flatResult.decorationsLength();
                    for (int di = 0; di < numDecorations; di++) {
                        var d = flatResult.decorations(di);
                        if (d == null) continue;
                        var rect = new GuideRenderPrimitive.FillRect(
                            Math.round(d.x()),
                            Math.round(d.y()),
                            Math.round(d.w()),
                            Math.round(d.h()),
                            (int) d.argb());
                        if (d.kind() == 3) {
                            separatorsByNode.computeIfAbsent((int) d.node(), k -> new ArrayList<>())
                                .add(rect);
                        } else if (d.kind() == 0) {
                            backgroundsByNode.computeIfAbsent((int) d.node(), k -> new ArrayList<>())
                                .add(rect);
                        } else {
                            linesByNode.computeIfAbsent((int) d.node(), k -> new ArrayList<>())
                                .add(rect);
                        }
                    }
                    for (var entry : runsByNode.entrySet()) {
                        LytNode node = serializer.getNodeByFlatIndex(entry.getKey());
                        if (node instanceof GlyphRunHolder holder) {
                            holder.setGlyphData(new GlyphRunData(
                                entry.getValue(),
                                backgroundsByNode.getOrDefault(entry.getKey(), List.of()),
                                linesByNode.getOrDefault(entry.getKey(), List.of()),
                                separatorsByNode.getOrDefault(entry.getKey(), List.of())));
                        }
                    }
                    // Post pass: blocks that derive state from children's final
                    // bounds (e.g. ordered-list numbers) re-apply it now.
                    for (int i = 0; i < numLayouts; i++) {
                        LytNode node = serializer.getNodeByFlatIndex(i);
                        if (node instanceof LytBlock lb) {
                            lb.afterExternalLayout();
                        }
                    }
                    return;
                }
            } catch (Exception e) {
                GuideDebugLog.warnAlways(
                    "[GuideNH-Mermaid] NodeContent Rust layout failed; falling back to Java layout", e);
            }
        }
        // Fallback: Java manual layout (the LytVBox stub is not a real pass).
        layoutContentSubtree(localContext, block, contentWidth);
    }

    /**
     * Recursively lay out all LytVBox containers inside {@code block},
     * including nested ones (LytList / LytListItem / LytVBox), so every block
     * in the subtree obtains non-empty bounds visible to
     * {@link #resolveBlockVisualBounds} and the primitive collector. This is
     * the Java fallback used when the Rust layout engine is unavailable — the
     * normal document pipeline and the NodeContent Rust pass never reach it.
     * <p>
     * Uses <b>post-order</b> traversal: subtrees are laid out first, then
     * siblings are positioned via {@link Layouts#verticalLayout}. This ordering
     * is required because {@link LytList} and {@link LytListItem} have real
     * {@code computeBoxLayout} that recursively lay out children; a pre-order
     * pass would re-layout those children a second time at incorrect
     * coordinates (offset relative to 0 instead of the parent's actual Y
     * position).
     */
    protected static void layoutContentSubtree(LayoutContext context, LytBlock block, int contentWidth) {
        if (!(block instanceof LytVBox vbox)) return;
        List<LytBlock> blockChildren = new ArrayList<>();
        for (LytNode child : vbox.getChildren()) {
            if (child instanceof LytBlock b) blockChildren.add(b);
        }
        if (blockChildren.isEmpty()) return;
        // Post-order: lay out child subtrees before positioning siblings.
        for (LytBlock child : blockChildren) {
            layoutContentSubtree(context, child, contentWidth);
        }
        // Content VBox created by compileNodeContentBlock has default
        // padding (0), gap (0), and alignItems (START).
        Layouts.verticalLayout(context, blockChildren,
            0, 0, contentWidth,
            0, 0, 0, 0,
            vbox.getGap(), vbox.getAlignItems());
    }

    /**
     * Recursively wipe glyph runs in the subtree so a failed Rust pass never
     * leaves stale runs rendering at outdated coordinates (mirrors
     * {@code LytDocument.clearGlyphRuns}).
     */
    private static void clearGlyphRuns(LytBlock block) {
        if (block instanceof GlyphRunHolder holder) {
            holder.setGlyphData(null);
        }
        for (LytNode child : block.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                clearGlyphRuns(childBlock);
            }
        }
    }

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
     * and a screen-space content area, then renders the block clipped to
     * {@code innerViewport ∩ contentArea} (the node's inner content boundary,
     * NOT the centered contentViewport) to prevent text overflow beyond the
     * node bounds.
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
        // Scissor uses node contentArea (not centered contentViewport) to
        // prevent text overflow beyond the node's inner boundary.
        LytRect innerViewport = getInnerViewport();
        LytRect clip = intersect(innerViewport, contentArea);
        if (clip == null) return;
        int originX = contentViewport.x() - Math.round(contentLayout.visualBounds().x() * activeZoom);
        int originY = contentViewport.y() - Math.round(contentLayout.visualBounds().y() * activeZoom);
        c.pushScissor(clip.x(), clip.y(), clip.width(), clip.height());
        c.pushTransform(originX, originY, activeZoom);
        c.collectFrom(contentLayout.block());
        c.popTransform();
        c.popScissor();
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
