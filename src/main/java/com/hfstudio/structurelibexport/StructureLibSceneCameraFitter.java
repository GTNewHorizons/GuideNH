package com.hfstudio.structurelibexport;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideProjectionMath;

public class StructureLibSceneCameraFitter {

    public static final int PADDING_PIXELS = 16;

    public FittedCamera fit(GuidebookLevel level, StructureLibExportTaskSpec task) {
        return fit(level, task.getPixelsPerBlock(), task.getScale(), task.getView());
    }

    public FittedCamera fit(GuidebookLevel level, int pixelsPerBlock, float scale, StructureLibExportView view) {
        int[] bounds = level.getBounds();
        int blockPixels = Math.max(1, Math.round(pixelsPerBlock * scale));
        CameraSettings camera = new CameraSettings();
        StructureLibExportView effectiveView = view != null ? view : StructureLibExportView.defaultView();
        effectiveView.apply(camera);
        float centerX = (bounds[0] + bounds[3] + 1f) * 0.5f;
        float centerY = (bounds[1] + bounds[4] + 1f) * 0.5f;
        float centerZ = (bounds[2] + bounds[5] + 1f) * 0.5f;
        camera.setRotationCenter(centerX, centerY, centerZ);
        camera.setZoom(blockPixels / 10f);
        camera.setViewportSize(1024, 1024);

        ProjectionBounds projectionBounds = projectBounds(camera, bounds);
        int width = Math.max(16, (int) Math.ceil(projectionBounds.width()) + PADDING_PIXELS * 2);
        int height = Math.max(16, (int) Math.ceil(projectionBounds.height()) + PADDING_PIXELS * 2);
        camera.setViewportSize(width, height);
        projectionBounds = projectBounds(camera, bounds);
        camera.setOffsetX(-(projectionBounds.minX + projectionBounds.maxX) * 0.5f);
        camera.setOffsetY((projectionBounds.minY + projectionBounds.maxY) * 0.5f);
        return new FittedCamera(camera, width, height);
    }

    private ProjectionBounds projectBounds(CameraSettings camera, int[] bounds) {
        float lx = bounds[0];
        float ly = bounds[1];
        float lz = bounds[2];
        float hx = bounds[3] + 1f;
        float hy = bounds[4] + 1f;
        float hz = bounds[5] + 1f;
        float[] projected = new float[4];
        GuideProjectionMath.projectAabbToScreen(
            camera.getCombinedMatrix(),
            camera.getViewportSize()
                .width(),
            camera.getViewportSize()
                .height(),
            lx,
            ly,
            lz,
            hx,
            hy,
            hz,
            projected);
        return new ProjectionBounds(projected[0], projected[1], projected[2], projected[3]);
    }

    @Desugar
    public record FittedCamera(CameraSettings camera, int width, int height) {}

    @Desugar
    public record ProjectionBounds(float minX, float maxX, float minY, float maxY) {

        public float width() {
            return maxX - minX;
        }

        public float height() {
            return maxY - minY;
        }
    }
}
