package com.hfstudio.guidenh.guide.internal.debug;

import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Interface for extracting debug information from specific LytNode types.
 * Implementations can be registered to provide extensible debug info extraction.
 */
public interface DebugInfoExtractor {

    /**
     * Check if this extractor can handle the given node type.
     */
    boolean canHandle(LytNode node);

    /**
     * Extract debug information from the node and add it to the info container.
     */
    void extract(LytNode node, HoveredElementInfo info);

    /**
     * Get the priority of this extractor. Higher priority extractors are checked first.
     * Default priority is 0.
     */
    default int getPriority() {
        return 0;
    }
}
