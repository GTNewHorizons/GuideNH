package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Collects {@link GuideRenderPrimitive} from a {@link LytNode} tree.
 *
 * <p>
 * Each node's {@link LytBlock#computePrimitives} emits its own primitives.
 * The collector recurses {@link LytNode#getChildren()} to traverse the document
 * tree, calling {@link LytBlock#emitDecorations} after children so that borders
 * paint over child content (matching the existing {@code LytBox.render} order).
 *
 * <p>
 * A transform stack tracks coordinate system changes for culling.
 * {@link GuideRenderPrimitive.PushTransform}/{@link GuideRenderPrimitive.PopTransform}
 * primitives are emitted in parallel so the render engine can reproduce the
 * same coordinate transforms at draw time.
 */
public class PrimitiveCollector {

    private final List<GuideRenderPrimitive> primitives = new ArrayList<>();
    private final LytRect viewport;
    private final Deque<CullFrame> cullStack = new ArrayDeque<>();

    public PrimitiveCollector(LytRect viewport) {
        this.viewport = viewport;
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
     * primitive list.
     */
    public void pushTransform(int dx, int dy, float scale) {
        CullFrame top = cullStack.peek();
        cullStack.push(
            new CullFrame(top.dx + Math.round(dx * top.scale), top.dy + Math.round(dy * top.scale), top.scale * scale));
        primitives.add(new GuideRenderPrimitive.PushTransform(dx, dy, scale));
    }

    /** Pop the last pushed transform from both stacks. */
    public void popTransform() {
        if (cullStack.size() > 1) {
            cullStack.pop();
        }
        primitives.add(new GuideRenderPrimitive.PopTransform());
    }

    // ---- emit ------------------------------------------------------------

    public void emit(GuideRenderPrimitive p) {
        primitives.add(p);
    }

    // ---- culling ---------------------------------------------------------

    /**
     * Returns {@code true} when {@code localBounds} (in the current transform
     * frame) lies completely outside the document-space viewport.
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
     * {@link LytNode#getChildren() descendants}.
     *
     * <p>
     * Callers that need a non-identity transform should push it
     * <em>before</em> calling this method (e.g. MermaidCanvas pushing an ELK
     * position offset for each content block).
     */
    public void collectFrom(LytNode root) {
        if (root instanceof LytBlock block) {
            if (isCulled(block.getBounds())) {
                return;
            }
            block.computePrimitives(this);
        }
        for (LytNode child : root.getChildren()) {
            collectFrom(child);
        }
        if (root instanceof LytBlock block) {
            block.emitDecorations(this);
        }
    }

    // ---- result ----------------------------------------------------------

    public List<GuideRenderPrimitive> result() {
        return List.copyOf(primitives);
    }
}
