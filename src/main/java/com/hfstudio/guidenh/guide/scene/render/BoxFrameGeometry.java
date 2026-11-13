package com.hfstudio.guidenh.guide.scene.render;

import java.nio.FloatBuffer;

import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;

/**
 * Emits the boundary of the edge-cuboid union without internal or overlapping faces.
 */
public class BoxFrameGeometry {

    private final float[] shape = new float[7];
    // The 3x3x3 edge-cell union has at most 72 boundary quads.
    private final float[] faces = new float[72 * 8];
    private final FaceConsumer collector = this::append;
    private int faceCount;
    private boolean initialized;
    private boolean verticesDirty;
    private FloatBuffer vertices;
    private FloatBuffer vertexView;

    public void update(Vector3fc min, Vector3fc max, float halfThickness) {
        if (initialized && shape[0] == min.x()
            && shape[1] == min.y()
            && shape[2] == min.z()
            && shape[3] == max.x()
            && shape[4] == max.y()
            && shape[5] == max.z()
            && shape[6] == halfThickness) return;
        shape[0] = min.x();
        shape[1] = min.y();
        shape[2] = min.z();
        shape[3] = max.x();
        shape[4] = max.y();
        shape[5] = max.z();
        shape[6] = halfThickness;
        faceCount = 0;
        forEachFace(min, max, halfThickness, collector);
        initialized = true;
        verticesDirty = true;
    }

    /**
     * Returns reusable, read-only C3F_V3F data for a single client-array draw.
     */
    public FloatBuffer vertices() {
        if (vertices == null) {
            vertices = BufferUtils.createFloatBuffer(faces.length / 8 * 4 * 6);
            vertices.limit(0);
            vertexView = vertices.asReadOnlyBuffer();
        }
        if (verticesDirty) {
            vertices.clear();
            for (int index = 0; index < faceCount; index++) {
                int offset = index * 8;
                float x0 = faces[offset], y0 = faces[offset + 1], z0 = faces[offset + 2];
                float x1 = faces[offset + 3], y1 = faces[offset + 4], z1 = faces[offset + 5];
                int axis = (int) faces[offset + 6], sign = (int) faces[offset + 7];
                float shade = axis == 1 ? (sign > 0 ? 1f : 128f / 255f) : (axis == 0 ? 0.6f : 0.8f);
                if (axis == 0 && sign < 0) {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x0, y0, z1);
                    vertex(shade, x0, y1, z1);
                    vertex(shade, x0, y1, z0);
                } else if (axis == 0) {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x0, y1, z0);
                    vertex(shade, x0, y1, z1);
                    vertex(shade, x0, y0, z1);
                } else if (axis == 1 && sign < 0) {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x1, y0, z0);
                    vertex(shade, x1, y0, z1);
                    vertex(shade, x0, y0, z1);
                } else if (axis == 1) {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x0, y0, z1);
                    vertex(shade, x1, y0, z1);
                    vertex(shade, x1, y0, z0);
                } else if (sign < 0) {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x0, y1, z0);
                    vertex(shade, x1, y1, z0);
                    vertex(shade, x1, y0, z0);
                } else {
                    vertex(shade, x0, y0, z0);
                    vertex(shade, x1, y0, z0);
                    vertex(shade, x1, y1, z0);
                    vertex(shade, x0, y1, z0);
                }
            }
            vertices.flip();
            vertexView.limit(vertices.limit());
            verticesDirty = false;
        }
        vertexView.position(0);
        return vertexView;
    }

    private void vertex(float shade, float x, float y, float z) {
        vertices.put(shade)
            .put(shade)
            .put(shade)
            .put(x)
            .put(y)
            .put(z);
    }

    public void forEachFace(FaceConsumer consumer) {
        for (int index = 0; index < faceCount; index++) {
            int offset = index * 8;
            consumer.accept(
                faces[offset],
                faces[offset + 1],
                faces[offset + 2],
                faces[offset + 3],
                faces[offset + 4],
                faces[offset + 5],
                (int) faces[offset + 6],
                (int) faces[offset + 7]);
        }
    }

    private void append(float x0, float y0, float z0, float x1, float y1, float z1, int axis, int sign) {
        int offset = faceCount++ * 8;
        faces[offset] = x0;
        faces[offset + 1] = y0;
        faces[offset + 2] = z0;
        faces[offset + 3] = x1;
        faces[offset + 4] = y1;
        faces[offset + 5] = z1;
        faces[offset + 6] = axis;
        faces[offset + 7] = sign;
    }

    @FunctionalInterface
    public interface FaceConsumer {

        void accept(float x0, float y0, float z0, float x1, float y1, float z1, int axis, int sign);
    }

    public static void forEachFace(Vector3fc min, Vector3fc max, float halfThickness, FaceConsumer consumer) {
        int nx = cellCount(min.x(), max.x(), halfThickness);
        int ny = cellCount(min.y(), max.y(), halfThickness);
        int nz = cellCount(min.z(), max.z(), halfThickness);
        for (int x = 0; x < nx; x++) {
            float x0 = coordinate(min.x(), max.x(), halfThickness, nx, x);
            float x1 = coordinate(min.x(), max.x(), halfThickness, nx, x + 1);
            for (int y = 0; y < ny; y++) {
                float y0 = coordinate(min.y(), max.y(), halfThickness, ny, y);
                float y1 = coordinate(min.y(), max.y(), halfThickness, ny, y + 1);
                for (int z = 0; z < nz; z++) {
                    if (!occupied(x, y, z, nx, ny, nz)) continue;
                    float z0 = coordinate(min.z(), max.z(), halfThickness, nz, z);
                    float z1 = coordinate(min.z(), max.z(), halfThickness, nz, z + 1);
                    if (!occupied(x - 1, y, z, nx, ny, nz)) consumer.accept(x0, y0, z0, x0, y1, z1, 0, -1);
                    if (!occupied(x + 1, y, z, nx, ny, nz)) consumer.accept(x1, y0, z0, x1, y1, z1, 0, 1);
                    if (!occupied(x, y - 1, z, nx, ny, nz)) consumer.accept(x0, y0, z0, x1, y0, z1, 1, -1);
                    if (!occupied(x, y + 1, z, nx, ny, nz)) consumer.accept(x0, y1, z0, x1, y1, z1, 1, 1);
                    if (!occupied(x, y, z - 1, nx, ny, nz)) consumer.accept(x0, y0, z0, x1, y1, z0, 2, -1);
                    if (!occupied(x, y, z + 1, nx, ny, nz)) consumer.accept(x0, y0, z1, x1, y1, z1, 2, 1);
                }
            }
        }
    }

    private static int cellCount(float min, float max, float halfThickness) {
        return max - min > 2f * halfThickness ? 3 : 1;
    }

    private static float coordinate(float min, float max, float halfThickness, int count, int index) {
        if (index == 0) return min - halfThickness;
        if (index == count) return max + halfThickness;
        return index == 1 ? min + halfThickness : max - halfThickness;
    }

    private static boolean occupied(int x, int y, int z, int nx, int ny, int nz) {
        if (x < 0 || x >= nx || y < 0 || y >= ny || z < 0 || z >= nz) return false;
        int edges = nx == 1 || x != 1 ? 1 : 0;
        if (ny == 1 || y != 1) edges++;
        if (nz == 1 || z != 1) edges++;
        return edges >= 2;
    }
}
