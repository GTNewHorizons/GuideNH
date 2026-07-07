package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class ContainerInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytList || node instanceof LytListItem || node instanceof LytTable;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        if (node instanceof LytList list) {
            info.addExtraInfo("Type: List");
            info.addExtraInfo(
                "Items: " + list.getChildren()
                    .size());
        } else if (node instanceof LytListItem) {
            info.addExtraInfo("Type: List Item");
        } else if (node instanceof LytTable table) {
            info.addExtraInfo("Type: Table");
            info.addExtraInfo(
                "Rows: " + table.getChildren()
                    .size());
        }
    }
}
