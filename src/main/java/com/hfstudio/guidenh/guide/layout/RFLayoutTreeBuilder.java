package com.hfstudio.guidenh.guide.layout;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.*;
import com.hfstudio.guidenh.guide.document.flow.*;
import com.hfstudio.guidenh.guide.document.block.table.*;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.layout.flatbuffers.FlatNode;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Style;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextStyle;
import com.hfstudio.guidenh.guide.layout.flatbuffers.TextData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Serializes a Lyt document tree into a FlatBuffer LayoutInput byte array.
 * Phase 1: minimal implementation — container nodes only.
 * Full elimination rules (FlowSpan, AlignedBlock, DocumentFloat, etc.) to follow.
 */
public class RFLayoutTreeBuilder {

    private final List<LytNode> flatNodes = new ArrayList<>();
    private final Map<LytNode, Integer> nodeToIndex = new IdentityHashMap<>();

    public byte[] serialize(LytNode root, float availWidth, float visualScale) {
        flatNodes.clear();
        nodeToIndex.clear();

        flattenTree(root);

        FlatBufferBuilder fbb = new FlatBufferBuilder(4096);
        int[] nodeOffsets = new int[flatNodes.size()];
        for (int i = 0; i < flatNodes.size(); i++) {
            nodeOffsets[i] = serializeNode(fbb, flatNodes.get(i));
        }

        int nodesVec = fbb.createVectorOfTables(nodeOffsets);
        int inputOff = com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput.createLayoutInput(
            fbb, availWidth, visualScale, nodesVec);
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
        int idx = flatNodes.size();
        flatNodes.add(node);
        nodeToIndex.put(node, idx);
        for (LytNode child : node.getChildren()) {
            flattenTree(child);
        }
    }

    private int serializeNode(FlatBufferBuilder fbb, LytNode node) {
        int styleOff = buildStyle(fbb);
        int[] childIndices = node.getChildren().stream()
            .mapToInt(c -> nodeToIndex.getOrDefault(c, -1))
            .filter(i -> i >= 0)
            .toArray();
        int childrenVec = createChildrenVector(fbb, childIndices);

        return FlatNode.createFlatNode(fbb, styleOff, (byte) 0,
            0, 0, 0, 0, 0, 0,
            (byte) 0,
            childrenVec);
    }

    // createChildrenVector helper — FlatBuffers [uint] maps to int vector
    private static int createChildrenVector(FlatBufferBuilder fbb, int[] indices) {
        fbb.startVector(4, indices.length, 4);
        for (int i = indices.length - 1; i >= 0; i--) {
            fbb.addInt(indices[i]);
        }
        return fbb.endVector();
    }

    private int buildStyle(FlatBufferBuilder fbb) {
        return Style.createStyle(fbb,
            (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0f,
            0f, 0f, 0f, 0f, false, false, false, false,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            (byte) 0,
            0f, 1f, 0,
            (byte) 0, (byte) 0,
            (byte) 0,
            0, 0, 0, 0);
    }
}
