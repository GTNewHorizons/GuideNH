package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Registry for debug info extractors.
 * Allows modular registration of extractors for different element types.
 */
public class DebugInfoExtractorRegistry {

    private static final List<DebugInfoExtractor> extractors = new ArrayList<>();
    private static boolean sorted = false;

    /**
     * Register a debug info extractor.
     */
    public static void register(DebugInfoExtractor extractor) {
        extractors.add(extractor);
        sorted = false;
    }

    /**
     * Extract debug info from a node using registered extractors.
     */
    public static void extract(LytNode node, HoveredElementInfo info) {
        ensureSorted();
        for (DebugInfoExtractor extractor : extractors) {
            if (extractor.canHandle(node)) {
                extractor.extract(node, info);
                return;
            }
        }
    }

    /**
     * Check if any extractor can handle the given node.
     */
    public static boolean canExtract(LytNode node) {
        ensureSorted();
        for (DebugInfoExtractor extractor : extractors) {
            if (extractor.canHandle(node)) {
                return true;
            }
        }
        return false;
    }

    private static void ensureSorted() {
        if (!sorted) {
            extractors.sort(
                Comparator.comparingInt(DebugInfoExtractor::getPriority)
                    .reversed());
            sorted = true;
        }
    }

    /**
     * Clear all registered extractors. Used for testing.
     */
    public static void clear() {
        extractors.clear();
        sorted = false;
    }

    /**
     * Get the number of registered extractors.
     */
    public static int size() {
        return extractors.size();
    }
}
