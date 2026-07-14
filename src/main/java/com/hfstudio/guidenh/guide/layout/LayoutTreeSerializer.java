package com.hfstudio.guidenh.guide.layout;

import java.util.*;

import javax.annotation.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.*;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.*;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Serializes a Lyt document tree into a FlatBuffer LayoutInput byte array.
 * <p>
 * Handles:
 * - Tree traversal and flat_index assignment
 * - Node elimination (spans, anchors, aligned blocks, floats)
 * - Paragraph merging (multiple LytFlowContent → one text FlatNode)
 * - Delegates Style build to {@link LayoutStyleExtractor}
 * - Delegates FlatNode build to {@link LayoutNodeSerializer}
 * <p>
 * This replaces RFLayoutTreeBuilder.
 */
public class LayoutTreeSerializer {

    private final List<LytBlock> flatNodes = new ArrayList<>();
    private final Map<LytNode, Integer> nodeToIndex = new IdentityHashMap<>();
    /** Margins accumulated from eliminated ancestors, applied during style extraction. */
    private final Map<LytBlock, MarginAccum> marginOffsets = new IdentityHashMap<>();

    public byte[] serialize(LytNode root, float availWidth, float visualScale) {
        flatNodes.clear();
        nodeToIndex.clear();
        marginOffsets.clear();

        flattenTree(root, MarginAccum.ZERO);

        FlatBufferBuilder fbb = new FlatBufferBuilder(4096);
        int[] nodeOffsets = new int[flatNodes.size()];

        // DEBUG: log flat node count and types
        long paraCount = flatNodes.stream().filter(b -> b instanceof LytParagraph).count();
        long imgCount = flatNodes.stream().filter(b -> b instanceof LytImage || b instanceof LytImageBlock).count();
        GuideDebugLog.warnAlways(
            "Layout: serializing {} flat nodes ({} para, {} img)",
            flatNodes.size(), paraCount, imgCount);

        for (int i = 0; i < flatNodes.size(); i++) {
            LytBlock block = flatNodes.get(i);
            MarginAccum mo = marginOffsets.getOrDefault(block, MarginAccum.ZERO);
            int styleOff = LayoutStyleExtractor.build(fbb, block,
                (int) mo.left(), (int) mo.right(), (int) mo.bottom(), (int) mo.top());
            List<Integer> childIndices = getChildIndices(block);
            nodeOffsets[i] = LayoutNodeSerializer.build(fbb, block, styleOff, childIndices);
        }

        int nodesVec = fbb.createVectorOfTables(nodeOffsets);
        int inputOff = LayoutInput.createLayoutInput(fbb, availWidth, visualScale, nodesVec);
        fbb.finish(inputOff);
        return fbb.sizedByteArray();
    }

    @Nullable
    public LytNode getNodeByFlatIndex(int index) {
        return index >= 0 && index < flatNodes.size() ? flatNodes.get(index) : null;
    }

    public int getFlatIndex(LytNode node) {
        return nodeToIndex.getOrDefault(node, -1);
    }

    /**
     * Accumulated margins from eliminated intermediate nodes that should be
     * pushed onto the nearest non-eliminated descendant block's own margins.
     */
    private record MarginAccum(float top, float right, float bottom, float left) {

        static MarginAccum ZERO = new MarginAccum(0, 0, 0, 0);

        MarginAccum add(LytBlock block) {
            return new MarginAccum(
                top + block.getMarginTop(),
                right + block.getMarginRight(),
                bottom + block.getMarginBottom(),
                left + block.getMarginLeft());
        }
    }

    private void flattenTree(LytNode node) {
        flattenTree(node, MarginAccum.ZERO);
    }

    private void flattenTree(LytNode node, MarginAccum inherited) {
        if (shouldEliminate(node)) {
            // Add this node's margins to the inherited accumulator
            MarginAccum total = inherited;
            if (node instanceof LytBlock elided) {
                total = total.add(elided);
            }
            for (LytNode child : node.getChildren()) {
                flattenTree(child, total);
            }
            return;
        }

        // Assign index and register
        int idx = flatNodes.size();
        // Only LytBlock subclasses can be flat nodes; skip non-block content
        if (node instanceof LytBlock block) {
            if (inherited != MarginAccum.ZERO) {
                marginOffsets.put(block, inherited);
            }
            flatNodes.add(block);
            nodeToIndex.put(node, idx);
        }

        for (LytNode child : node.getChildren()) {
            flattenTree(child, MarginAccum.ZERO);
        }
    }

    private boolean shouldEliminate(LytNode node) {
        // Flow classes don't extend LytNode — use class name check
        String name = node.getClass()
            .getName();
        if (name.contains("LytFlowSpan") || name.contains("LytFlowAnchor")
            || name.contains("LytFlowBreak")
            || name.contains("LytFlowInlineBlock")) {
            return true;
        }
        // Blocks that are layout wrappers — eliminated in tree
        if (node instanceof LytAlignedBlock || node instanceof LytDocumentFloat || node instanceof LytTableRow) {
            return true;
        }
        return false;
    }

    private List<Integer> getChildIndices(LytBlock block) {
        List<Integer> indices = new ArrayList<>();
        collectBlockChildren(block, indices);
        return indices;
    }

    /**
     * Collect flat-node indices for all {@link LytBlock} descendants of
     * {@code node}, skipping eliminated intermediate nodes.
     */
    private void collectBlockChildren(LytNode node, List<Integer> out) {
        for (LytNode child : node.getChildren()) {
            if (shouldEliminate(child)) {
                // Skip eliminated wrapper, descend into its children
                collectBlockChildren(child, out);
            } else {
                Integer idx = nodeToIndex.get(child);
                if (idx != null) {
                    out.add(idx);
                }
            }
        }
    }
}
