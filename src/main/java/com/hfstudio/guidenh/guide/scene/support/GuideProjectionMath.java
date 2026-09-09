package com.hfstudio.guidenh.guide.scene.support;

import org.joml.Matrix4f;

/**
 * Allocation-free projection helpers shared by scene rendering and export.
 * The scene camera uses an orthographic projection, so an AABB can be
 * projected by transforming its center and expanding by the absolute matrix
 * contribution of each half-extent instead of transforming all eight corners.
 */
public final class GuideProjectionMath {

    private GuideProjectionMath() {}

    /**
     * Projects an AABB into screen coordinates centered at the viewport origin.
     * The destination receives {@code {minX, maxX, minY, maxY}} and must have at
     * least four elements.
     */
    public static void projectAabbToScreen(Matrix4f matrix, float viewportWidth, float viewportHeight, float minX,
        float minY, float minZ, float maxX, float maxY, float maxZ, float[] destination) {
        if (destination == null || destination.length < 4) {
            throw new IllegalArgumentException("destination must contain at least four elements");
        }

        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        float extentX = (maxX - minX) * 0.5f;
        float extentY = (maxY - minY) * 0.5f;
        float extentZ = (maxZ - minZ) * 0.5f;

        float projectedCenterX = matrix.m00() * centerX + matrix.m10() * centerY
            + matrix.m20() * centerZ
            + matrix.m30();
        float projectedCenterY = matrix.m01() * centerX + matrix.m11() * centerY
            + matrix.m21() * centerZ
            + matrix.m31();
        float projectedRadiusX = Math.abs(matrix.m00()) * extentX + Math.abs(matrix.m10()) * extentY
            + Math.abs(matrix.m20()) * extentZ;
        float projectedRadiusY = Math.abs(matrix.m01()) * extentX + Math.abs(matrix.m11()) * extentY
            + Math.abs(matrix.m21()) * extentZ;

        float halfWidth = viewportWidth * 0.5f;
        float halfHeight = viewportHeight * 0.5f;
        float screenCenterX = projectedCenterX * halfWidth;
        float screenCenterY = -projectedCenterY * halfHeight;
        float screenRadiusX = projectedRadiusX * halfWidth;
        float screenRadiusY = projectedRadiusY * halfHeight;
        destination[0] = screenCenterX - screenRadiusX;
        destination[1] = screenCenterX + screenRadiusX;
        destination[2] = screenCenterY - screenRadiusY;
        destination[3] = screenCenterY + screenRadiusY;
    }

    public static boolean intersectsCenteredViewport(float[] projectedBounds, float viewportWidth,
        float viewportHeight) {
        return projectedBounds[1] >= -viewportWidth * 0.5f && projectedBounds[0] <= viewportWidth * 0.5f
            && projectedBounds[3] >= -viewportHeight * 0.5f
            && projectedBounds[2] <= viewportHeight * 0.5f;
    }
}
