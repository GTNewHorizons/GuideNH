package com.hfstudio.guidenh.guide.scene.outline;

import org.joml.Matrix4fc;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Strategy that turns a resolved {@link OutlineFrame} into pixels. Called inside the scene's render
 * context, with the scene's projection, view matrix, viewport and scissor still bound.
 */
interface OutlineRenderBackend {

    void render(GuidebookLevel level, OutlineFrame frame, OutlineTargetDrawer drawer, Matrix4fc viewProjection,
        float partialTicks);

    void release();
}
