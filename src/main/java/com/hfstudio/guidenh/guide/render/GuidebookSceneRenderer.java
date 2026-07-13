package com.hfstudio.guidenh.guide.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

/**
 * Renders GuidebookScene 3D content.
 * <p>
 * Phase 1: minimal GL state management shell.
 * Full 3D rendering integration with GuidebookLevelRenderer
 * will be wired when RenderScene3D primitives enter the pipeline.
 */
public class GuidebookSceneRenderer {

    /**
     * Phase 1: stub. Sets viewport and saves/restores GL state.
     * 
     * @param level  GuidebookLevel (the "fake world")
     * @param camera CameraSettings
     */
    public void render(Object level, Object camera, List<?> particles, List<?> weatherEffects,
        float weatherAnimationTick, Object lightDarkMode, int clipX, int clipY, int clipW, int clipH, int viewportW,
        int viewportH) {
        if (level == null) return;

        GL11.glViewport(clipX, clipY, Math.max(1, clipW), Math.max(1, clipH));

        // Phase 1: no-op
        // Full implementation delegates to GuidebookLevelRenderer
        // and CameraSettings after extracting from VanillaRenderContext.
    }
}
