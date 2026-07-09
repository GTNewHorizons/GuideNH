package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class MermaidInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytMermaidMindmap;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        LytMermaidMindmap mermaid = (LytMermaidMindmap) node;
        info.addExtraInfo("Type: Mermaid Mindmap");

        var canvas = mermaid.getCanvas();
        if (canvas != null && canvas.getBounds() != null) {
            int canvasWidth = canvas.getBounds()
                .width();
            int canvasHeight = canvas.getBounds()
                .height();
            if (canvasWidth > 0 || canvasHeight > 0) {
                info.addExtraInfo("Canvas: " + canvasWidth + "x" + canvasHeight);
            }
        }

        String sourceText = mermaid.getSourceText();
        if (sourceText != null && !sourceText.isEmpty()) {
            info.addExtraInfo("Source: " + sourceText.length() + " chars");
        }

        int children = mermaid.getChildren()
            .size();
        if (children > 0) {
            info.addExtraInfo("Children: " + children);
        }
    }
}
