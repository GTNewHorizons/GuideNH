package com.hfstudio.guidenh.guide.document.block;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.recipes.LytRecipeGalleryRow;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.interaction.DocumentInteractionSnapshot;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.LayoutTreeSerializer;
import com.hfstudio.guidenh.guide.layout.Layouts;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutResult;
import com.hfstudio.guidenh.guide.render.GlyphRunData;
import com.hfstudio.guidenh.guide.render.GlyphRunGroup;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.render.GuideRenderEngine;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuidebookSceneRenderer;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Getter;

/**
 * Layout document. Has a viewport and an overall size which may exceed the document size vertically, but not
 * horizontally.
 */
public class LytDocument extends LytNode implements LytBlockContainer {

    private static final FlowInteractionPath EMPTY_FLOW_PATH = FlowInteractionPath.empty();

    private static volatile GuideRenderEngine renderEngine;

    public static GuideRenderEngine getRenderEngine() {
        GuideRenderEngine e = renderEngine;
        if (e == null) {
            synchronized (LytDocument.class) {
                e = renderEngine;
                if (e == null) {
                    renderEngine = e = new GuideRenderEngine(GuideGlyphAtlas.instance(), new GuidebookSceneRenderer());
                }
            }
        }
        return e;
    }

    @Getter
    private final List<LytBlock> blocks = new ArrayList<>();

    @Nullable
    private Layout layout;

    /** Cache key state for {@link #updateLayout}: font handle + render scale. */
    private long layoutFontHandle = -1;
    private int layoutRenderScale = -1;

    @Nullable
    private DocumentInteractionSnapshot hoveredElement;

    @Getter
    private boolean live;

    // Cached list of blocks intersecting the last rendered viewport. Invalidated whenever the
    // block list mutates or the layout is rebuilt; kept across frames otherwise so scrolling at
    // a steady viewport position only pays the iteration cost once.
    private final List<LytBlock> visibleCache = new ArrayList<>();
    private int cachedViewportTop = Integer.MIN_VALUE;
    private int cachedViewportBottom = Integer.MIN_VALUE;
    private boolean visibleCacheValid;

    /**
     * Diagnostic overlay (JVM flag {@code -Dguidenh.layoutOverlay=true} or the
     * in-game debug menu option): draws Java bounds (green), Rust FlatLayout
     * rects (red), glyph quads (blue) and the viewport scissor (yellow) on top
     * of the document. Resolved per call so the in-game toggle takes effect
     * immediately.
     */
    private static boolean isOverlayEnabled() {
        return GuideDebugLog.isLayoutOverlayEnabled();
    }

    private final List<LytBlock> overlayRustBlocks = new ArrayList<>();
    private final List<LytRect> overlayRustRects = new ArrayList<>();

    public int getAvailableWidth() {
        return layout != null ? layout.availableWidth() : 0;
    }

    public int getContentHeight() {
        return layout != null ? layout.contentHeight() : 0;
    }

    @Override
    public List<LytBlock> getChildren() {
        return blocks;
    }

    @Override
    public @Nullable LytRect getBounds() {
        return layout != null ? layout.bounds() : null;
    }

    @Override
    public void removeChild(LytNode node) {
        if (node instanceof LytBlock block) {
            if (block.parent == this) {
                block.parent = null;
            }
            blocks.remove(block);
            invalidateLayout();
        }
    }

    @Override
    public void append(LytBlock block) {
        if (block.parent != null) {
            block.parent.removeChild(block);
        }
        block.parent = this;
        blocks.add(block);
        invalidateLayout();
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newNode) {
        if (oldChild instanceof LytBlock oldBlock) {
            int idx = blocks.indexOf(oldBlock);
            if (idx < 0) return;
            oldBlock.parent = null;
            if (newNode instanceof LytBlock newBlock) {
                if (newBlock.parent != null) {
                    newBlock.parent.removeChild(newBlock);
                }
                newBlock.parent = this;
                blocks.set(idx, newBlock);
            }
            invalidateLayout();
        }
    }

    public void clearContent() {
        for (var block : blocks) {
            block.parent = null;
        }
        blocks.clear();
        invalidateLayout();
    }

    public boolean hasLayout() {
        return layout != null;
    }

    public void setLive(boolean live) {
        if (this.live == live) return;
        this.live = live;
        cascadeLive(this, live);
    }

    private static void cascadeLive(LytNode node, boolean live) {
        if (live) {
            node.onAttach();
        } else {
            node.onDetach();
        }
        for (var child : node.getChildren()) {
            cascadeLive(child, live);
        }
    }

    static void notifyAttach(LytNode node) {
        cascadeLive(node, true);
    }

    static void notifyDetach(LytNode node) {
        cascadeLive(node, false);
    }

    public void invalidateLayout() {
        layout = null;
        invalidateVisibleCache();
    }

    private void invalidateVisibleCache() {
        visibleCacheValid = false;
        visibleCache.clear();
    }

    public void updateLayout(LayoutContext context, int availableWidth) {
        // Cache key must include the font handle (a 0→non-zero transition must
        // not leave the document stuck on the Java fallback) and the display
        // pixel ratio (glyph bitmaps are rasterized per render scale) — B-3.
        long fontHandle = com.hfstudio.guidenh.guide.layout.LayoutBridge.getFontHandle();
        int renderScale = com.hfstudio.guidenh.guide.internal.util.DisplayScale.scaleFactor();
        if (layout != null && layout.availableWidth == availableWidth
            && layoutFontHandle == fontHandle
            && layoutRenderScale == renderScale) {
            return;
        }
        layoutFontHandle = fontHandle;
        layoutRenderScale = renderScale;

        groupRecipeGalleries();
        layout = createLayout(context, availableWidth);
    }

    /**
     * Group runs of &ge; 2 consecutive recipe boxes at the document top level
     * into wrapping {@link LytRecipeGalleryRow}s, so recipes fill the available
     * width instead of stacking one per row. Idempotent: an already-grouped
     * document is left untouched (no mutation, no extra invalidation). Runs
     * before every layout so asynchronously inserted recipe boxes (placeholders
     * resolving late) join an adjacent gallery.
     */
    private void groupRecipeGalleries() {
        // Pass 1: existing gallery rows absorb immediately following single
        // recipe boxes (manual index — the list shrinks on each absorb).
        for (int i = 0; i + 1 < blocks.size();) {
            if (blocks.get(i) instanceof LytRecipeGalleryRow row
                && LytRecipeGalleryRow.isRecipeBox(blocks.get(i + 1))) {
                row.append(blocks.get(i + 1)); // re-parents the box off the document
                invalidateLayout();
            } else {
                i++;
            }
        }
        // Pass 2: wrap remaining runs of >= 2 consecutive recipe boxes.
        for (int i = 0; i < blocks.size(); i++) {
            if (!LytRecipeGalleryRow.isRecipeBox(blocks.get(i))) continue;
            int j = i + 1;
            while (j < blocks.size() && LytRecipeGalleryRow.isRecipeBox(blocks.get(j))) j++;
            if (j - i >= 2) {
                List<LytBlock> run = new ArrayList<>(blocks.subList(i, j));
                var row = new LytRecipeGalleryRow();
                for (LytBlock box : run) {
                    row.append(box); // re-parents each box off the document
                }
                row.parent = this;
                blocks.add(i, row);
                invalidateLayout();
            }
            i = j; // continue after the run — a single box does NOT stop the scan
        }
    }

    private Layout createLayout(LayoutContext context, int availableWidth) {
        // Java layout pass: still needed so the serializer can read inline-block
        // visual sizes and opaque-container bounds (SIZE_FROM_JAVA_BOUNDS). The
        // Rust pipeline overwrites all geometry afterward.
        Layouts.verticalLayout(context, blocks, 0, 0, availableWidth, 5, 5, 5, 5, 0, AlignItems.START);
        context.clearFloats(true, true);

        // --- Rust layout pipeline (sole authority for geometry) ---
        int contentHeight = 0;
        long fontHandle = LayoutBridge.getFontHandle();
        // Clear glyph runs up front, across the WHOLE tree (not just the
        // serialized nodes): on any Rust failure/empty result, no stale
        // run may survive to be drawn at outdated coordinates (B-2/B-11).
        for (LytBlock top : blocks) {
            clearGlyphRuns(top);
        }
        try {
            var serializer = new LayoutTreeSerializer();
            byte[] input = serializer
                .serialize(this, availableWidth, context.getVisualScale(), DisplayScale.scaleFactor());
            long t0 = System.nanoTime();
            byte[] result = LayoutBridge.measureLayout(fontHandle, input);
            long elapsed = System.nanoTime() - t0;
            GuideDebugLog.warnAlways("Layout: measureLayout took {} ms", elapsed / 1_000_000);
            if (result.length > 0) {
                var flatResult = LayoutResult.getRootAsLayoutResult(ByteBuffer.wrap(result));
                String debugInfo = flatResult.debugInfo();
                if (debugInfo != null && !debugInfo.isEmpty()) {
                    GuideDebugLog.warnAlways("Layout: Rust debug_info = {}", debugInfo);
                }
                contentHeight = (int) flatResult.contentHeight();
                // Layout landing: overwrite every serialized block's bounds with
                // the Rust-computed rect so blocks, glyph runs and floats share
                // one coordinate truth. The FlatLayout vector is index-aligned
                // with the serializer's flat nodes.
                overlayRustBlocks.clear();
                overlayRustRects.clear();
                int numLayouts = flatResult.nodesLength();
                for (int i = 0; i < numLayouts; i++) {
                    var fl = flatResult.nodes(i);
                    if (fl == null) continue;
                    LytNode node = serializer.getNodeByFlatIndex(i);
                    if (!(node instanceof LytBlock lb)) continue;
                    LytRect rustRect = new LytRect(
                        Math.round(fl.x()),
                        Math.round(fl.y()),
                        Math.max(0, Math.round(fl.w())),
                        Math.max(0, Math.round(fl.h())));
                    lb.applyExternalLayout(rustRect);
                    if (isOverlayEnabled()) {
                        overlayRustBlocks.add(lb);
                        overlayRustRects.add(rustRect);
                    }
                }
                // Post pass: let blocks re-apply state that depends on their
                // children's final bounds (e.g. scroll offsets) BEFORE the
                // cull-bounds union is recomputed from the moved subtrees.
                for (int i = 0; i < numLayouts; i++) {
                    LytNode node = serializer.getNodeByFlatIndex(i);
                    if (node instanceof LytBlock lb) {
                        lb.afterExternalLayout();
                    }
                }
                // Upload unique glyph bitmaps (hi-res, rasterized by Rust at
                // render_scale) to the atlas. Placement is already baked into
                // the per-glyph quads below.
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
                    atlas.upload(bmp.key(), rgba, w, h);
                }
                // Extract glyph runs (final document-space quads, top-left
                // origin) and group them per paragraph node — a rich
                // paragraph yields one run per span — then inject together
                // with the span decoration rects.
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
                            placed.add(
                                new GuideRenderPrimitive.PlacedGlyph(
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
                    (d.kind() == 0 ? backgroundsByNode : linesByNode)
                        .computeIfAbsent((int) d.node(), k -> new ArrayList<>())
                        .add(rect);
                }
                for (var entry : runsByNode.entrySet()) {
                    LytNode node = serializer.getNodeByFlatIndex(entry.getKey());
                    if (node instanceof GlyphRunHolder holder) {
                        holder.setGlyphData(
                            new GlyphRunData(
                                entry.getValue(),
                                backgroundsByNode.getOrDefault(entry.getKey(), List.of()),
                                linesByNode.getOrDefault(entry.getKey(), List.of())));
                        GuideDebugLog.warnAlways(
                            "Layout: set {} glyph groups on paragraph at flat index {}",
                            entry.getValue()
                                .size(),
                            entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            GuideDebugLog.warnAlways("Layout: Rust pipeline failed", e);
        }
        // Culling bounds: subtree union, recomputed on EVERY layout (success or
        // fallback — stale unions cull content that moved into view, B-2). A
        // floated child (or any overflowing content) must keep its ancestors
        // visible — Taffy does not extend containers for floats.
        for (LytBlock top : blocks) {
            computeCullBounds(top);
        }
        // -----------------------------------------

        var cachedBounds = new LytRect(0, 0, availableWidth, contentHeight);
        return new Layout(availableWidth, contentHeight, cachedBounds);
    }

    /**
     * Recursively clear glyph runs in the subtree (pre-layout wipe so no stale
     * run survives a failed layout pass).
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
     * Recompute the subtree-union cull bounds for {@code block} (post-order:
     * children first). Returns the union rect, also stored on the block.
     */
    private static LytRect computeCullBounds(LytBlock block) {
        LytRect union = block.getBounds() != null ? block.getBounds() : LytRect.empty();
        for (LytNode child : block.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                union = LytRect.union(union, computeCullBounds(childBlock));
            }
        }
        block.setCullBounds(union);
        return union;
    }

    public void render(RenderContext context) {
        var viewport = context.viewport();
        var top = viewport.y();
        var bottom = top + viewport.height();
        if (!visibleCacheValid || top != cachedViewportTop || bottom != cachedViewportBottom) {
            visibleCache.clear();
            for (var block : blocks) {
                if (!block.isCulled(viewport)) {
                    visibleCache.add(block);
                }
            }
            cachedViewportTop = top;
            cachedViewportBottom = bottom;
            visibleCacheValid = true;
        }
        // Primitive pipeline. The render engine owns the document->screen
        // conversion: root transform maps screen = doc * zoom + (tx, ty),
        // matching VanillaRenderContext.toScreenRect. The screen viewport scissor
        // is fixed on screen regardless of scroll/zoom, so it is emitted in
        // screen coordinates (PushScreenScissor) before the root transform.
        float zoom = context.getZoom();
        float scrollY = context.getPreciseScrollOffsetY();
        int originX = context.getDocumentOriginX();
        int originY = context.getDocumentOriginY();
        LytRect screenViewport = context.getScreenViewport();

        var engine = getRenderEngine();
        engine.beginFrame(screenViewport, DisplayScale.scaleFactor());
        var pc = new PrimitiveCollector(screenViewport, context);
        pc.pushScreenScissor(screenViewport.x(), screenViewport.y(), screenViewport.width(), screenViewport.height());
        pc.pushTransform(originX, originY - scrollY * zoom, zoom);
        try {
            for (LytBlock lb : visibleCache) {
                pc.collectFrom(lb);
            }
        } finally {
            pc.popTransform();
            pc.popScreenScissor();
        }
        var prims = pc.result();
        if (!prims.isEmpty()) {
            engine.execute(prims);
        }
        engine.endFrame();
        if (isOverlayEnabled()) {
            renderLayoutOverlay(originX, originY - scrollY * zoom, zoom, screenViewport, pc.getCulledDocRects());
        }
    }

    // ---- diagnostic layout overlay (JVM flag or in-game debug menu option) ---

    private void renderLayoutOverlay(float tx, float ty, float zoom, LytRect screenViewport,
        List<LytRect> culledRects) {
        // Clip all overlay drawing to the viewport: without this, rects of
        // offscreen/culled blocks bleed into the screen and look like empty panels.
        var mc = net.minecraft.client.Minecraft.getMinecraft();
        int s = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            screenViewport.x() * s,
            Math.max(0, mc.displayHeight - screenViewport.bottom() * s),
            screenViewport.width() * s,
            screenViewport.height() * s);
        try {
            // Yellow: fixed viewport scissor (screen space)
            drawScreenOutline(
                screenViewport.x(),
                screenViewport.y(),
                screenViewport.width(),
                screenViewport.height(),
                0xFFFFFF00);
            // Green: Java-side block bounds; Blue: glyph quads
            overlayWalk(this, tx, ty, zoom);
            // Red: Rust FlatLayout rects
            for (LytRect r : overlayRustRects) {
                drawDocOutline(r.x(), r.y(), r.width(), r.height(), tx, ty, zoom, 0xFFFF0000);
            }
            // Gray: blocks culled this frame (correctly not rendered)
            for (LytRect r : culledRects) {
                drawDocOutline(r.x(), r.y(), r.width(), r.height(), tx, ty, zoom, 0xFF888888);
            }
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void overlayWalk(LytNode node, float tx, float ty, float zoom) {
        if (node instanceof LytBlock block) {
            LytRect b = block.getBounds();
            if (b != null) {
                drawDocOutline(b.x(), b.y(), b.width(), b.height(), tx, ty, zoom, 0xFF00FF00);
            }
            if (block instanceof LytParagraph paragraph && paragraph.getGlyphData() != null) {
                for (var group : paragraph.getGlyphData()
                    .runs()) {
                    for (var g : group.glyphs()) {
                        drawDocOutline(
                            Math.round(g.x()),
                            Math.round(g.y()),
                            Math.round(g.w()),
                            Math.round(g.h()),
                            tx,
                            ty,
                            zoom,
                            0xFF0000FF);
                    }
                }
            }
        }
        for (LytNode child : node.getChildren()) {
            overlayWalk(child, tx, ty, zoom);
        }
    }

    private void drawDocOutline(int x, int y, int w, int h, float tx, float ty, float zoom, int argb) {
        drawScreenOutline(
            Math.round(x * zoom + tx),
            Math.round(y * zoom + ty),
            Math.max(1, Math.round(w * zoom)),
            Math.max(1, Math.round(h * zoom)),
            argb);
    }

    private static void drawScreenOutline(int x, int y, int w, int h, int argb) {
        net.minecraft.client.gui.Gui.drawRect(x, y, x + w, y + 1, argb);
        net.minecraft.client.gui.Gui.drawRect(x, y + h - 1, x + w, y + h, argb);
        net.minecraft.client.gui.Gui.drawRect(x, y + 1, x + 1, y + h - 1, argb);
        net.minecraft.client.gui.Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    public @Nullable DocumentInteractionSnapshot getHoveredElement() {
        return hoveredElement;
    }

    public void setHoveredElement(@Nullable DocumentInteractionSnapshot hoveredElement) {
        if (!Objects.equals(hoveredElement, this.hoveredElement)) {
            if (this.hoveredElement != null) {
                if (this.hoveredElement.node() != null) {
                    applyInteractionSnapshot(this.hoveredElement.node(), null);
                    this.hoveredElement.node()
                        .onMouseLeave();
                }
            }
            this.hoveredElement = hoveredElement;
            if (this.hoveredElement != null && this.hoveredElement.node() != null) {
                applyInteractionSnapshot(this.hoveredElement.node(), this.hoveredElement);
                this.hoveredElement.node()
                    .onMouseEnter(hoveredElement.primaryHoverTarget());
            }
        }
    }

    private void applyInteractionSnapshot(LytNode node, @Nullable DocumentInteractionSnapshot snapshot) {
        if (node instanceof LytParagraph paragraph) {
            FlowInteractionPath hoverPath = snapshot != null ? snapshot.flowPath() : EMPTY_FLOW_PATH;
            FlowInteractionPath revealPath = snapshot != null && snapshot.activeSpoiler() != null
                ? new FlowInteractionPath(snapshot.activeSpoiler(), snapshot.revealTargets(), snapshot.activeSpoiler())
                : EMPTY_FLOW_PATH;
            paragraph.setInteractionPaths(hoverPath, revealPath);
        }
    }

    public @Nullable DocumentInteractionSnapshot pick(int x, int y) {
        return pick(this, x, y);
    }

    public static @Nullable DocumentInteractionSnapshot pick(LytNode root, int x, int y) {
        var node = root.pickNode(x, y);
        if (node != null) {
            FlowInteractionPath flowPath = EMPTY_FLOW_PATH;
            if (node instanceof LytFlowContainer container) {
                flowPath = container.pickContent(x, y);

                // If the content is an inline-block, we descend into it! (This can go on and on and on...)
                if (flowPath != null && flowPath.primary() instanceof LytFlowInlineBlock inlineBlock
                    && inlineBlock.getBlock() != null) {
                    return pick(inlineBlock.getBlock(), x, y);
                }
            }
            if (flowPath == null) {
                flowPath = EMPTY_FLOW_PATH;
            }
            var spoiler = flowPath.firstSpoiler();
            List<LytFlowContent> revealTargets = spoiler != null ? List.of(spoiler) : List.of();
            return new DocumentInteractionSnapshot(
                node,
                flowPath,
                flowPath.primary(),
                flowPath.primary(),
                flowPath.targets(),
                revealTargets,
                spoiler);
        }

        return null;
    }

    @Desugar
    public record Layout(int availableWidth, int contentHeight, LytRect bounds) {}

}
