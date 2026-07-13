package com.hfstudio.guidenh.guide.layout;

import java.util.*;

import javax.annotation.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.*;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.*;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;

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

    public byte[] serialize(LytNode root, float availWidth, float visualScale) {
        flatNodes.clear();
        nodeToIndex.clear();

        flattenTree(root);

        FlatBufferBuilder fbb = new FlatBufferBuilder(4096);
        int[] nodeOffsets = new int[flatNodes.size()];
        for (int i = 0; i < flatNodes.size(); i++) {
            LytBlock block = flatNodes.get(i);
            int styleOff = LayoutStyleExtractor.build(fbb, block);
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

    private void flattenTree(LytNode node) {
        if (shouldEliminate(node)) {
            // Skip this node, recurse into its children directly
            for (LytNode child : node.getChildren()) {
                flattenTree(child);
            }
            return;
        }

        // Assign index and register
        int idx = flatNodes.size();
        // Only LytBlock subclasses can be flat nodes; skip non-block content
        if (node instanceof LytBlock block) {
            flatNodes.add(block);
            nodeToIndex.put(node, idx);
        }

        for (LytNode child : node.getChildren()) {
            flattenTree(child);
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
        for (LytNode child : block.getChildren()) {
            Integer idx = nodeToIndex.get(child);
            if (idx != null) {
                indices.add(idx);
            }
        }
        return indices;
    }
}
