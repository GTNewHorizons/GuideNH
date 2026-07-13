package com.hfstudio.guidenh.guide.render;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Traverses the Lyt tree and collects render primitives for GuideRenderEngine.
 * <p>
 * Default traversal visits getChildren() recursively.
 * Nodes that render via non-child sub-trees (MermaidCanvas, FlowInlineBlock)
 * must override collectPrimitives() and call {@link #collectDelegated(LytNode)}.
 */
public class GuidePrimitiveCollector {

    private final LytRect viewport;

    public GuidePrimitiveCollector(LytRect viewport) {
        this.viewport = viewport;
    }

    /** Collect primitives from the root node and its descendants. */
    public List<GuideRenderPrimitive> collect(LytNode root) {
        List<GuideRenderPrimitive> out = new ArrayList<>();
        collectRecursive(root, out, true);
        return out;
    }

    /**
     * Collect primitives from a delegated sub-tree (not a child in the Taffy tree).
     * <p>
     * Skips viewport culling — the caller has already set up a PushTransform/PushScissor
     * to establish the correct coordinate system for this sub-tree.
     */
    public List<GuideRenderPrimitive> collectDelegated(LytNode node) {
        List<GuideRenderPrimitive> out = new ArrayList<>();
        collectRecursive(node, out, false);
        return out;
    }

    private void collectRecursive(LytNode node, List<GuideRenderPrimitive> out, boolean doCull) {
        if (doCull) {
            LytRect b = node.getBounds();
            if (b == null || isCulled(b)) return;
        }

        // Node's own primitives.
        // TODO: Phase 1 — add collectPrimitives() to LytNode when node migration begins.
        // For now, nodes are rendered by the existing VanillaRenderContext path.

        // Children
        for (LytNode child : node.getChildren()) {
            collectRecursive(child, out, doCull);
        }
    }

    private boolean isCulled(LytRect b) {
        return b.right() < viewport.x() || b.bottom() < viewport.y()
            || b.x() > viewport.right()
            || b.y() > viewport.bottom();
    }

    public LytRect getViewport() {
        return viewport;
    }
}
