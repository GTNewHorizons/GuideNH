package com.hfstudio.guidenh.guide.scene.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.joml.Matrix4f;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection.Mode;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideProjectionMath;

public class GuideSceneBlockVisibilityCache {

    private static final int AABB_CULLING_BLOCK_THRESHOLD = 128;
    private static final float CONTAINMENT_MARGIN_PIXELS = 1.0f;

    private final Map<GuidebookLevel, Visibility> levels = new WeakHashMap<>();
    private final Visibility uncached = new Visibility();
    private final float[] projectedBounds = new float[4];

    public List<int[]> collect(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection selection) {
        // Subclasses may implement visibility rules that are not represented by the standard cache key.
        boolean cacheable = level.getClass() == GuidebookLevel.class && camera.getClass() == CameraSettings.class
            && selection.getClass() == GuidebookSceneLayerSelection.class;
        Visibility visibility = cacheable ? levels.computeIfAbsent(level, ignored -> new Visibility()) : uncached;
        Matrix4f matrix = camera.getCombinedMatrix();
        long revision = level.getSpatialRevision();
        int width = camera.getViewportSize()
            .width();
        int height = camera.getViewportSize()
            .height();
        if (cacheable && visibility.valid
            && visibility.revision == revision
            && visibility.width == width
            && visibility.height == height
            && visibility.mode == selection.getMode()
            && visibility.layers.equals(selection.getVisibleLayers())
            && visibility.matrix.equals(matrix)) {
            return visibility.view;
        }

        var filledBlocks = level.getFilledBlocks();
        boolean skipCulling = selection.getMode() == Mode.ALL && filledBlocks.size() <= AABB_CULLING_BLOCK_THRESHOLD;
        boolean allBlocksVisible = skipCulling
            || (cacheable && selection.getMode() == Mode.ALL && containsScene(level, matrix, width, height));
        boolean reuseAllBlocks = cacheable && visibility.valid
            && visibility.revision == revision
            && visibility.allBlocksVisible
            && allBlocksVisible;
        visibility.valid = false;
        if (!reuseAllBlocks) {
            visibility.blocks.clear();
            for (int[] position : filledBlocks) {
                if (selection.isLayerVisible(position[1]) && (allBlocksVisible || camera.isAabbPotentiallyVisible(
                    position[0],
                    position[1],
                    position[2],
                    position[0] + 1.0f,
                    position[1] + 1.0f,
                    position[2] + 1.0f))) {
                    visibility.blocks.add(position);
                }
            }
        }
        if (cacheable) {
            visibility.matrix.set(matrix);
            visibility.revision = revision;
            visibility.width = width;
            visibility.height = height;
            visibility.mode = selection.getMode();
            visibility.layers = selection.getVisibleLayers();
            visibility.allBlocksVisible = allBlocksVisible;
            visibility.valid = true;
        }
        return visibility.view;
    }

    public void release(GuidebookLevel level) {
        levels.remove(level);
        uncached.blocks.clear();
    }

    public void clear() {
        levels.clear();
        uncached.blocks.clear();
    }

    private boolean containsScene(GuidebookLevel level, Matrix4f matrix, int width, int height) {
        if (width <= 0 || height <= 0) {
            return true;
        }
        int[] bounds = level.getBounds();
        GuideProjectionMath.projectAabbToScreen(
            matrix,
            width,
            height,
            bounds[0],
            bounds[1],
            bounds[2],
            bounds[3] + 1.0f,
            bounds[4] + 1.0f,
            bounds[5] + 1.0f,
            projectedBounds);
        float halfWidth = width * 0.5f - CONTAINMENT_MARGIN_PIXELS;
        float halfHeight = height * 0.5f - CONTAINMENT_MARGIN_PIXELS;
        for (float value : projectedBounds) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return projectedBounds[0] >= -halfWidth && projectedBounds[1] <= halfWidth
            && projectedBounds[2] >= -halfHeight
            && projectedBounds[3] <= halfHeight;
    }

    private static class Visibility {

        private final ArrayList<int[]> blocks = new ArrayList<>();
        private final List<int[]> view = Collections.unmodifiableList(blocks);
        private final Matrix4f matrix = new Matrix4f();
        private long revision;
        private int width;
        private int height;
        private Mode mode;
        private Set<Integer> layers = Set.of();
        private boolean valid;
        private boolean allBlocksVisible;
    }
}
