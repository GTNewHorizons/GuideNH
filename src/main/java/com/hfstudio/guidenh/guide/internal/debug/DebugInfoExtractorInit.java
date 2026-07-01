package com.hfstudio.guidenh.guide.internal.debug;

import com.hfstudio.guidenh.guide.internal.debug.extractors.ChartInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.CodeBlockInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.ContainerInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.DocumentInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.GenericBlockInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.HeadingInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.ImageInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.ItemInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.LatexInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.MermaidInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.ParagraphInfoExtractor;
import com.hfstudio.guidenh.guide.internal.debug.extractors.SceneInfoExtractor;

/**
 * Initializes and registers all built-in debug info extractors.
 * This class should be called once during client initialization.
 */
public class DebugInfoExtractorInit {

    private static boolean initialized = false;

    /**
     * Register all built-in extractors.
     * This method is idempotent and can be safely called multiple times.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        DebugInfoExtractorRegistry.register(new DocumentInfoExtractor());
        DebugInfoExtractorRegistry.register(new HeadingInfoExtractor());
        DebugInfoExtractorRegistry.register(new ParagraphInfoExtractor());
        DebugInfoExtractorRegistry.register(new ItemInfoExtractor());
        DebugInfoExtractorRegistry.register(new SceneInfoExtractor());
        DebugInfoExtractorRegistry.register(new MermaidInfoExtractor());
        DebugInfoExtractorRegistry.register(new ChartInfoExtractor());
        DebugInfoExtractorRegistry.register(new CodeBlockInfoExtractor());
        DebugInfoExtractorRegistry.register(new LatexInfoExtractor());
        DebugInfoExtractorRegistry.register(new ImageInfoExtractor());
        DebugInfoExtractorRegistry.register(new ContainerInfoExtractor());
        DebugInfoExtractorRegistry.register(new GenericBlockInfoExtractor());

        initialized = true;
    }

    /**
     * Check if extractors have been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
