package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class LatexInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytLatexBlock || node instanceof LytLatexDisplayBlock;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        if (node instanceof LytLatexDisplayBlock) {
            info.addExtraInfo("Type: Latex Display Block");
        } else {
            info.addExtraInfo("Type: Latex Block");
        }
    }
}
