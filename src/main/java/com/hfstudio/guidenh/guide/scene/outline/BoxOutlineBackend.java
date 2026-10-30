package com.hfstudio.guidenh.guide.scene.outline;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;

import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotationRenderer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;

/**
 * Fallback for contexts without framebuffer or GLSL support: draws the targets' bounding boxes with the
 * same visible/occluded convention as box annotations.
 */
final class BoxOutlineBackend implements OutlineRenderBackend {

    private final Vector3f min = new Vector3f();
    private final Vector3f max = new Vector3f();

    @Override
    public void render(GuidebookLevel level, OutlineFrame frame, OutlineTargetDrawer drawer, Matrix4fc viewProjection,
        float partialTicks) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDepthFunc(GL11.GL_GREATER);
            drawFrame(level, frame, true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            drawFrame(level, frame, false);
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glPopAttrib();
        }
    }

    @Override
    public void release() {}

    private void drawFrame(GuidebookLevel level, OutlineFrame frame, boolean occluded) {
        for (int i = 0; i < frame.batchCount(); i++) {
            OutlineBatch batch = frame.batch(i);
            int argb = occluded ? batch.getOccludedArgb() : batch.getVisibleArgb();
            drawTargets(level, batch.getSolid(), argb, batch.getThickness());
            drawTargets(level, batch.getCutout(), argb, batch.getThickness());
        }
    }

    private void drawTargets(GuidebookLevel level, OutlineTargetSet targets, int argb, float thickness) {
        drawBlocks(level, targets.blocks(), argb, thickness);
        for (Entity entity : targets.entities()) {
            if (entity.boundingBox != null) {
                drawBox(entity.boundingBox, argb, thickness);
            }
        }
        float[] boxes = targets.boxes();
        for (int i = 0; i < targets.boxCount(); i++) {
            int o = i * 6;
            min.set(boxes[o], boxes[o + 1], boxes[o + 2]);
            max.set(boxes[o + 3], boxes[o + 4], boxes[o + 5]);
            InWorldAnnotationRenderer.drawBoxEdges(min, max, argb, thickness);
        }
    }

    private void drawBlocks(GuidebookLevel level, List<int[]> blocks, int argb, float thickness) {
        for (int[] position : blocks) {
            AxisAlignedBB bounds = GuideBlockBoundsResolver
                .resolveSelectedBounds(level, position[0], position[1], position[2]);
            if (bounds != null) {
                drawBox(bounds, argb, thickness);
            }
        }
    }

    private void drawBox(AxisAlignedBB box, int argb, float thickness) {
        min.set((float) box.minX, (float) box.minY, (float) box.minZ);
        max.set((float) box.maxX, (float) box.maxY, (float) box.maxZ);
        InWorldAnnotationRenderer.drawBoxEdges(min, max, argb, thickness);
    }
}
