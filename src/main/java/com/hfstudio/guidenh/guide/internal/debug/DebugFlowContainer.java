package com.hfstudio.guidenh.guide.internal.debug;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;

/**
 * Exposes flow elements and their rendered bounds for debug picking.
 */
public interface DebugFlowContainer {

    /**
     * Picks the flow content at the given coordinates.
     *
     * @return FlowContentEntry with the picked content and its bounds, or null if none found
     */
    FlowContentEntry pickFlowContent(int x, int y);

    /**
     * Returns all flow content entries with their bounds.
     * Used for debugging and visualization.
     */
    List<FlowContentEntry> getAllFlowContent();

    /**
     * Represents a flow content element with its rendered bounds.
     */
    record FlowContentEntry(LytFlowContent content, LytRect bounds) {}
}
