package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

/**
 * Fallback extractor for generic block elements.
 * This has the lowest priority and only adds child count info.
 */
public class GenericBlockInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytBlock;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        LytBlock block = (LytBlock) node;
        int childCount = block.getChildren()
            .size();
        if (childCount > 0) {
            info.addExtraInfo("Children: " + childCount);
        }
    }

    @Override
    public int getPriority() {
        return -1000;
    }
}
