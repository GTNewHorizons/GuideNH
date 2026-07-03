package com.hfstudio.guidenh.guide.internal.debug.extractors;

import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.HoveredElementInfo;

public class HeadingInfoExtractor implements DebugInfoExtractor {

    @Override
    public boolean canHandle(LytNode node) {
        return node instanceof LytHeading;
    }

    @Override
    public void extract(LytNode node, HoveredElementInfo info) {
        LytHeading heading = (LytHeading) node;
        info.addExtraInfo("Type: Heading");
        info.addExtraInfo("Depth: " + heading.getDepth());
        String text = extractText(heading.getContent());
        if (!text.isEmpty()) {
            info.addExtraInfo("Text: " + truncate(text, 50));
        }
    }

    private String extractText(Iterable<? extends Object> contents) {
        StringBuilder sb = new StringBuilder();
        for (Object content : contents) {
            String text = String.valueOf(content);
            if (text != null && !text.isEmpty()) {
                sb.append(text);
            }
        }
        return sb.toString()
            .trim();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
