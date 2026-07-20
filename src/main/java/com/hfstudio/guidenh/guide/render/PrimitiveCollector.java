package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Collects {@link GuideRenderPrimitive} from a {@link LytNode} tree.
 *
 * <p>
 * <b>Traversal authority.</b> This collector owns tree traversal. A block's
 * {@link LytBlock#computePrimitives} must NOT iterate {@link LytNode#getChildren()}
 * — doing so double-renders the subtree. Blocks with private rendering data
 * (e.g. MermaidCanvas) may call {@link #collectFrom} on their internal subtrees
 * from within {@code computePrimitives}, typically after {@link #pushTransform}.
 *
 * <p>
 * <b>Per-block order</b> (in {@link #collectFrom}):
 * <ol>
 * <li>the block's own primitives via {@link LytBlock#computePrimitives}</li>
 * <li>{@link LytBlock#getChildrenClipRect()} → PushScissor (children only)</li>
 * <li>children, in order</li>
 * <li>PopScissor</li>
 * <li>{@link LytBlock#emitDecorations} — outside the children scissor, matching
 * the legacy {@code LytBox.render} order (background → children → border)</li>
 * </ol>
 *
 * <p>
 * <b>Legacy fallback.</b> Blocks whose {@link LytBlock#usePrimitives()} returns
 * false are rendered through a {@link GuideRenderPrimitive.HostDraw} primitive
 * that invokes their legacy {@link LytBlock#render} for the whole subtree; the
 * collector does not recurse into their children (the legacy path renders them
 * itself).
 *
 * <p>
 * <b>Culling.</b> The viewport passed to the constructor is in <em>screen GUI
 * coordinates</em> — the same space the transform stack maps into
 * ({@code screen = doc * scale + t}). A block whose transformed bounds lie
 * outside the viewport is skipped together with its whole subtree.
 */
public class PrimitiveCollector {

    private final List<GuideRenderPrimitive> primitives = new ArrayList<>();
    /** Viewport in screen GUI coordinates, used for culling. */
    private final LytRect viewport;
    private final RenderContext legacyContext;
    private final Deque<CullFrame> cullStack = new ArrayDeque<>();
    /**
     * Document-space bounds of blocks culled during the last traversal,
     * recorded only when the layout overlay is enabled (diagnostics).
     */
    private final List<LytRect> culledDocRects = new ArrayList<>();

    public PrimitiveCollector(LytRect viewport, RenderContext legacyContext) {
        this.viewport = viewport;
        this.legacyContext = legacyContext;
        cullStack.push(new CullFrame(0, 0, 1.0f));
    }

    // ---- transform stack (culling + primitive emission) ------------------

    private record CullFrame(int dx, int dy, float scale) {

        int x(int localX) {
            return dx + Math.round(localX * scale);
        }

        int y(int localY) {
            return dy + Math.round(localY * scale);
        }

        int w(int localW) {
            return Math.max(1, Math.round(localW * scale));
        }

        int h(int localH) {
            return Math.max(1, Math.round(localH * scale));
        }
    }

    /**
     * Push a translate + uniform scale onto both the culling stack and the
     * primitive list. The new frame maps {@code screen = doc * scale + (dx, dy)}
     * and composes with its parent, mirroring GuideRenderEngine's transform stack.
     */
    public void pushTransform(float dx, float dy, float scale) {
        CullFrame top = cullStack.peek();
        cullStack.push(
            new CullFrame(top.dx + Math.round(dx * top.scale), top.dy + Math.round(dy * top.scale), top.scale * scale));
        primitives.add(new GuideRenderPrimitive.PushTransform(dx, dy, scale));
    }

    /**
     * Pop the last pushed transform. Only emits PopTransform when a frame was
     * actually popped, keeping the engine's transform stack in sync.
     */
    public void popTransform() {
        if (cullStack.size() > 1) {
            cullStack.pop();
            primitives.add(new GuideRenderPrimitive.PopTransform());
        }
    }

    // ---- emit ------------------------------------------------------------

    public void emit(GuideRenderPrimitive p) {
        primitives.add(p);
    }

    /**
     * Emit a legacy-render fallback for {@code block}: the engine will invoke
     * {@link LytBlock#render} with a GL modelview that maps document coordinates
     * to screen, so unmigrated blocks render exactly as before.
     * <p>
     * Diagnostics (-Dguidenh.layoutOverlay=true): before the legacy render, paint
     * a magenta marker inset from the block's bounds and log class/bounds/scissor.
     * If the marker shows but the block doesn't, the bug is inside the block's
     * legacy render; if the marker also fails, the HostDraw GL/context setup is broken.
     */
    public void emitLegacy(LytBlock block) {
        primitives.add(new GuideRenderPrimitive.HostDraw(legacyContext, () -> {
            if (GuideDebugLog.isLayoutOverlayEnabled()) {
                var b = block.getBounds();
                GuideDebugLog.warnAlways(
                    "[TRC] legacy render {} bounds={} ctxScissor={}",
                    block.getClass()
                        .getSimpleName(),
                    b,
                    legacyContext.currentScissor());
                if (b != null && b.width() > 4 && b.height() > 4) {
                    legacyContext.fillRect(b.x() + 2, b.y() + 2, b.width() - 4, b.height() - 4, 0xFFFF00FF);
                }
            }
            block.render(legacyContext);
        }));
    }

    /**
     * Emit a PushScissor in document coordinates.
     * The render engine converts to screen coordinates via its transform stack.
     */
    public void pushScissor(int x, int y, int w, int h) {
        primitives.add(new GuideRenderPrimitive.PushScissor(x, y, w, h));
    }

    /** Emit a PopScissor primitive. */
    public void popScissor() {
        primitives.add(new GuideRenderPrimitive.PopScissor());
    }

    /**
     * Emit a scissor rectangle in <em>screen</em> GUI coordinates.
     * The render engine intersects it with the current scissor and applies the
     * display scale factor directly — no transform-stack conversion is applied.
     * This is intended for the fixed viewport clip, which must stay fixed
     * regardless of scroll offset or zoom (a document-space rect cannot express
     * it exactly when zoom != 1).
     */
    public void pushScreenScissor(int x, int y, int w, int h) {
        primitives.add(new GuideRenderPrimitive.PushScreenScissor(x, y, w, h));
    }

    /** Emit a PopScreenScissor primitive. */
    public void popScreenScissor() {
        primitives.add(new GuideRenderPrimitive.PopScreenScissor());
    }

    // ---- culling ---------------------------------------------------------

    /**
     * Returns {@code true} when {@code localBounds} (in the current transform
     * frame) lies completely outside the screen-space viewport.
     */
    public boolean isCulled(LytRect localBounds) {
        CullFrame tx = cullStack.peek();
        int x = tx.x(localBounds.x());
        int y = tx.y(localBounds.y());
        int r = x + tx.w(localBounds.width());
        int b = y + tx.h(localBounds.height());
        return r < viewport.x() || b < viewport.y() || x > viewport.right() || y > viewport.bottom();
    }

    public LytRect getViewport() {
        return viewport;
    }

    // ---- tree traversal --------------------------------------------------

    /**
     * Recursively collect primitives from {@code root} and its
     * {@link LytNode#getChildren() descendants}. See the class javadoc for the
     * traversal contract.
     *
     * <p>
     * Callers that need a non-identity transform should push it
     * <em>before</em> calling this method (e.g. MermaidCanvas pushing an ELK
     * position offset for each content block).
     */
    public void collectFrom(LytNode root) {
        LytRect clipRect = null;
        boolean doClip = false;
        if (root instanceof LytBlock block) {
            var b = block.getCullBounds();
            boolean culled = isCulled(b);
            if (culled) {
                if (GuideDebugLog.isLayoutOverlayEnabled()) {
                    if (b != null) culledDocRects.add(b);
                    GuideDebugLog.warnAlways(
                        "[TRC] culled {} bounds={} viewport={}",
                        block.getClass()
                            .getSimpleName(),
                        b,
                        viewport);
                }
                return;
            }
            if (!block.usePrimitives()) {
                // Legacy fallback: the block renders its whole subtree through
                // the old RenderContext path; do not recurse (double render).
                if (GuideDebugLog.isLayoutOverlayEnabled()) {
                    GuideDebugLog.warnAlways(
                        "[TRC] emitLegacy {} bounds={}",
                        block.getClass()
                            .getSimpleName(),
                        b);
                }
                emitLegacy(block);
                return;
            }
            block.computePrimitives(this);
            if (GuideDebugLog.isLayoutOverlayEnabled()) {
                GuideDebugLog.warnAlways(
                    "[TRC] primitives {} bounds={}",
                    block.getClass()
                        .getSimpleName(),
                    b);
            }
            clipRect = block.getChildrenClipRect();
            doClip = clipRect != null;
        }
        // Framework-managed scissor around children traversal
        if (doClip) {
            pushScissor(clipRect.x(), clipRect.y(), clipRect.width(), clipRect.height());
        }
        for (LytNode child : root.getChildren()) {
            collectFrom(child);
        }
        if (doClip) {
            popScissor();
        }
        if (root instanceof LytBlock block) {
            block.emitDecorations(this);
        }
    }

    // ---- result ----------------------------------------------------------

    public List<GuideRenderPrimitive> result() {
        return List.copyOf(primitives);
    }

    /** Document-space bounds of blocks culled during traversal (diagnostics). */
    public List<LytRect> getCulledDocRects() {
        return culledDocRects;
    }

    /** Clear all collected primitives and reset the cull stack. */
    public void clear() {
        primitives.clear();
        culledDocRects.clear();
        cullStack.clear();
        cullStack.push(new CullFrame(0, 0, 1.0f));
    }
}
