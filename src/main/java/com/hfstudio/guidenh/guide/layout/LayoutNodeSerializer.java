package com.hfstudio.guidenh.guide.layout;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.*;
import com.hfstudio.guidenh.guide.document.block.chart.*;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.recipes.*;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.layout.flatbuffers.*;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

import java.util.List;

/**
 * Determines node_type and builds FlatNode with the appropriate sub-data table.
 */
public final class LayoutNodeSerializer {

    private LayoutNodeSerializer() {}

    public static int build(FlatBufferBuilder fbb, LytBlock block,
                             int styleOff, List<Integer> childIndices) {
        byte nodeType = resolveNodeType(block);
        int textOff  = nodeType == 1 ? buildTextData(fbb, block) : 0;
        int imageOff = nodeType == 2 ? buildImageData(fbb, block) : 0;
        int slotOff  = nodeType == 3 ? buildSlotData(fbb, block) : 0;
        int breakOff = nodeType == 4 ? buildThematicBreakData(fbb) : 0;
        int customOff = nodeType == 5 ? buildCustomData(fbb, block) : 0;
        int latexOff = nodeType == 8 ? buildLatexData(fbb, block) : 0;
        byte customLayout = 0;

        int childrenVec = buildChildrenVector(fbb, childIndices);
        return FlatNode.createFlatNode(fbb, styleOff, nodeType,
            textOff, imageOff, slotOff, breakOff, customOff, latexOff,
            customLayout, childrenVec);
    }


    private static byte resolveNodeType(LytBlock block) {
        if (block instanceof LytThematicBreak) return 4;
        if (block instanceof LytImage || block instanceof LytImageBlock) return 2;
        if (block instanceof LytSlot) return 3;
        if (block instanceof LytLatexBlock || block instanceof LytLatexDisplayBlock) return 8;
        if (block instanceof LytTable) return 7;
        if (block instanceof LytFileTree) return 6;
        if (isCustomNode(block)) return 5;
        return 0; // Container
    }

    private static boolean isCustomNode(LytBlock block) {
        return block instanceof LytMermaidCanvas
            || block instanceof LytMermaidMindmap
            || block instanceof LytStructureView
            || block instanceof LytGuidebookScene
            || block instanceof LytContentTabsBlock
            || block instanceof LytGenericRecipeBox
            || block instanceof LytStandardRecipeBox
            || block instanceof LytNeiRecipeBox
            || block instanceof LytChartBase
            || block instanceof LytBarChart
            || block instanceof LytColumnChart
            || block instanceof LytLineChart
            || block instanceof LytScatterChart
            || block instanceof LytPieChart
            || block instanceof LytFunctionGraph;
    }


    private static int buildTextData(FlatBufferBuilder fbb, LytBlock block) {
        int strOff = fbb.createString("");
        int styleOff = TextStyle.createTextStyle(fbb, 14f, false, false, 1f, 0xFFFFFFFF, 0);
        return TextData.createTextData(fbb, strOff, styleOff, (byte) 0);
    }

    private static int buildImageData(FlatBufferBuilder fbb, LytBlock block) {
        return ImageData.createImageData(fbb, 0, 0, 0, 0, -1, -1, 1f, 1f, -1f, -1f);
    }

    private static int buildSlotData(FlatBufferBuilder fbb, LytBlock block) {
        return SlotData.createSlotData(fbb, 18f);
    }

    private static int buildThematicBreakData(FlatBufferBuilder fbb) {
        return ThematicBreakData.createThematicBreakData(fbb, 6f);
    }

    private static int buildCustomData(FlatBufferBuilder fbb, LytBlock block) {
        int typeId = resolveCustomTypeId(block);
        if (typeId < 0) return 0;
        fbb.startVector(1, 0, 1);
        int payload = fbb.endVector();
        return CustomData.createCustomData(fbb, typeId, payload);
    }

    private static int buildLatexData(FlatBufferBuilder fbb, LytBlock block) {
        int formulaOff = fbb.createString("");
        return LatexDisplayData.createLatexDisplayData(fbb, formulaOff,
            0xFFFFFFFF, 100f, 1f, 0, 0, 0f, 0f, 0f);
    }

    private static int resolveCustomTypeId(LytBlock block) {
        if (block instanceof LytMermaidCanvas) return 1;
        if (block instanceof LytStructureView) return 2;
        if (block instanceof LytChartBase || block instanceof LytBarChart
            || block instanceof LytColumnChart || block instanceof LytLineChart
            || block instanceof LytScatterChart || block instanceof LytPieChart) return 3;
        if (block instanceof LytGuidebookScene) return 4;
        if (block instanceof LytMermaidMindmap) return 5;
        if (block instanceof LytContentTabsBlock) return 6;
        if (block instanceof LytGenericRecipeBox
            || block instanceof LytStandardRecipeBox
            || block instanceof LytNeiRecipeBox) return 7;
        if (block instanceof LytFunctionGraph) return 3; // Chart type
        return -1;
    }


    private static int buildChildrenVector(FlatBufferBuilder fbb, List<Integer> indices) {
        fbb.startVector(4, indices.size(), 4);
        for (int i = indices.size() - 1; i >= 0; i--) {
            fbb.addInt(indices.get(i));
        }
        return fbb.endVector();
    }
}
