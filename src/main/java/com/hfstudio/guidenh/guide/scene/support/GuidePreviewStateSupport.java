package com.hfstudio.guidenh.guide.scene.support;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPreparePipeline;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;

/**
 * Cross-mod entry point for preparing guide preview state. Actual logic lives in registered
 * {@link PreviewPrepareContributor}s.
 */
public class GuidePreviewStateSupport {

    protected GuidePreviewStateSupport() {}

    public static void prepare(GuidebookLevel level) {
        PreviewPreparePipeline.prepare(level);
    }
}
