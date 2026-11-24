package com.hfstudio.guidenh.client.capture.render;

import java.util.Arrays;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

public class CaptureMeshReplayCommand implements CaptureReplayCommand {

    private final CaptureRecordedGlState state;
    private final int[] vertices;
    private final int stride;
    private final int mode;
    private final boolean textured;
    private final boolean colored;
    private final boolean lit;
    private final boolean normals;

    public CaptureMeshReplayCommand(CaptureTextureSnapshotStore textures, int[] raw, int count, int used, int mode,
        boolean textured, boolean colored, boolean lit, boolean normals) {
        if (count <= 0 || used <= 0 || used > raw.length || used % count != 0 || used / count < 8)
            throw new IllegalArgumentException("The renderer uses an unsupported vertex layout");
        state = new CaptureRecordedGlState(textures, false);
        vertices = Arrays.copyOf(raw, used);
        stride = used / count;
        this.mode = mode;
        this.textured = textured;
        this.colored = colored;
        this.lit = lit;
        this.normals = normals;
    }

    @Override
    public void draw(CaptureReplayTransform transform) {
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            state.apply(transform);
            GL11.glBegin(mode);
            try {
                for (int index = 0; index < vertices.length; index += stride) {
                    if (colored) {
                        int color = vertices[index + 5];
                        GL11.glColor4ub((byte) color, (byte) (color >> 8), (byte) (color >> 16), (byte) (color >> 24));
                    }
                    if (normals) {
                        int normal = vertices[index + 6];
                        GL11.glNormal3f(
                            Math.max(-1, (byte) normal / 127f),
                            Math.max(-1, (byte) (normal >> 8) / 127f),
                            Math.max(-1, (byte) (normal >> 16) / 127f));
                    }
                    if (lit) {
                        int brightness = vertices[index + 7];
                        OpenGlHelper.setLightmapTextureCoords(
                            OpenGlHelper.lightmapTexUnit,
                            brightness & 65535,
                            brightness >>> 16);
                    }
                    if (textured) GL11.glTexCoord2f(
                        Float.intBitsToFloat(vertices[index + 3]),
                        Float.intBitsToFloat(vertices[index + 4]));
                    GL11.glVertex3f(
                        Float.intBitsToFloat(vertices[index]),
                        Float.intBitsToFloat(vertices[index + 1]),
                        Float.intBitsToFloat(vertices[index + 2]));
                }
            } finally {
                GL11.glEnd();
            }
        }
    }
}
