package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class ItemInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytItemImage || node instanceof LytSlot;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        if (node instanceof LytItemImage) {
            info.addExtraInfo("Type: Item Display");
        } else if (node instanceof LytSlot) {
            info.addExtraInfo("Type: Interactive Slot");
        }
    }
}
