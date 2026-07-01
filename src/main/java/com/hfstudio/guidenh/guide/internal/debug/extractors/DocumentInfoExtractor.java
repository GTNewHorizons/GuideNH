package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class DocumentInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytDocument;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        LytDocument doc = (LytDocument) node;
        info.addExtraInfo("Type: Document Root");
        int blockCount = doc.getChildren()
            .size();
        info.addExtraInfo("Blocks: " + blockCount);
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
