package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Runs after tiles are bound into the fake world and before scene rendering. Contributors can
 * inspect {@link GuidebookLevel#previewRuntimeMutations()} when imported authority data must be
 * ignored for positions superseded by a runtime scene operation or animation.
 */
public interface PreviewPrepareContributor {

    int priority();

    void prepare(GuidebookLevel level);
}
