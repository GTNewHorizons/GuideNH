package com.hfstudio.guidenh.client.capture.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

public class CaptureRecordedGlState {

    private static final int[] ENABLES = { GL11.GL_BLEND, GL11.GL_ALPHA_TEST, GL11.GL_DEPTH_TEST, GL11.GL_CULL_FACE,
        GL11.GL_FOG, GL11.GL_LIGHTING, GL11.GL_NORMALIZE, GL12.GL_RESCALE_NORMAL, GL11.GL_COLOR_MATERIAL };
    private static final int[] SCALARS = { GL11.GL_DEPTH_FUNC, GL11.GL_CULL_FACE_MODE, GL11.GL_FRONT_FACE,
        GL11.GL_SHADE_MODEL, GL11.GL_ALPHA_TEST_FUNC, GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB,
        GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA, GL14.GL_BLEND_EQUATION, GL11.GL_FOG_MODE,
        GL11.GL_COLOR_MATERIAL_FACE, GL11.GL_COLOR_MATERIAL_PARAMETER };
    private static final int[] LIGHT_PARAMETERS = { GL11.GL_POSITION, GL11.GL_AMBIENT, GL11.GL_DIFFUSE,
        GL11.GL_SPECULAR, GL11.GL_SPOT_DIRECTION, GL11.GL_SPOT_CUTOFF, GL11.GL_SPOT_EXPONENT,
        GL11.GL_CONSTANT_ATTENUATION, GL11.GL_LINEAR_ATTENUATION, GL11.GL_QUADRATIC_ATTENUATION };
    private static final int[] MATERIAL_PARAMETERS = { GL11.GL_AMBIENT, GL11.GL_DIFFUSE, GL11.GL_SPECULAR,
        GL11.GL_EMISSION, GL11.GL_SHININESS };
    private final boolean[] enabled = new boolean[ENABLES.length];
    private final int[] scalars = new int[SCALARS.length];
    private final float[] projection;
    private final float[] model;
    private final int[] viewport;
    private final int[] scissor;
    private final boolean scissorEnabled;
    private final boolean[] colorMask = new boolean[4];
    private final boolean depthMask;
    private final float alphaReference;
    private final float lineWidth;
    private final float pointSize;
    private final float[] color;
    private final float[] normal;
    private final float[] fogColor;
    private final float fogDensity;
    private final float fogStart;
    private final float fogEnd;
    private final float[] lightModel;
    private final int twoSided;
    private final List<Light> lights = new ArrayList<>();
    private final float[][] material;
    private final TextureUnit primary;
    private final TextureUnit lightmap;
    private final float brightnessX;
    private final float brightnessY;

    public CaptureRecordedGlState(CaptureTextureSnapshotStore textures, boolean text) {
        int shader = GLContext.getCapabilities().OpenGL20 ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
            && (GL11.glGetInteger(GL11.GL_STENCIL_FUNC) != GL11.GL_ALWAYS
                || GL11.glGetInteger(GL11.GL_STENCIL_FAIL) != GL11.GL_KEEP
                || GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL) != GL11.GL_KEEP
                || GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS) != GL11.GL_KEEP);
        boolean logic = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        if (!GLContext.getCapabilities().OpenGL14 || stencil || logic || shader != 0)
            throw new IllegalArgumentException(
                "This draw requires a dedicated snapshot provider: shader=" + shader
                    + ", stencil="
                    + stencil
                    + ", logic="
                    + logic);
        for (int index = 0; index < enabled.length; index++) enabled[index] = GL11.glIsEnabled(ENABLES[index]);
        for (int index = 0; index < scalars.length; index++) scalars[index] = GL11.glGetInteger(SCALARS[index]);
        projection = floats(GL11.GL_PROJECTION_MATRIX, 16);
        model = floats(GL11.GL_MODELVIEW_MATRIX, 16);
        viewport = integers(GL11.GL_VIEWPORT);
        scissor = integers(GL11.GL_SCISSOR_BOX);
        scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        ByteBuffer mask = BufferUtils.createByteBuffer(4);
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, mask);
        for (int index = 0; index < 4; index++) colorMask[index] = mask.get(index) != 0;
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        pointSize = GL11.glGetFloat(GL11.GL_POINT_SIZE);
        color = floats(GL11.GL_CURRENT_COLOR, 4);
        normal = floats(GL11.GL_CURRENT_NORMAL, 3);
        fogColor = floats(GL11.GL_FOG_COLOR, 4);
        fogDensity = GL11.glGetFloat(GL11.GL_FOG_DENSITY);
        fogStart = GL11.glGetFloat(GL11.GL_FOG_START);
        fogEnd = GL11.glGetFloat(GL11.GL_FOG_END);
        lightModel = floats(GL11.GL_LIGHT_MODEL_AMBIENT, 4);
        twoSided = GL11.glGetInteger(GL11.GL_LIGHT_MODEL_TWO_SIDE);
        material = new float[MATERIAL_PARAMETERS.length * 2][];
        if (enabled[5]) {
            for (int light = 0; light < 8; light++)
                if (GL11.glIsEnabled(GL11.GL_LIGHT0 + light)) lights.add(new Light(light));
            for (int side = 0; side < 2; side++) {
                for (int index = 0; index < MATERIAL_PARAMETERS.length; index++) {
                    FloatBuffer values = BufferUtils.createFloatBuffer(4);
                    GL11.glGetMaterial(side == 0 ? GL11.GL_FRONT : GL11.GL_BACK, MATERIAL_PARAMETERS[index], values);
                    int count = MATERIAL_PARAMETERS[index] == GL11.GL_SHININESS ? 1 : 4;
                    float[] copied = new float[count];
                    values.get(copied);
                    material[side * MATERIAL_PARAMETERS.length + index] = copied;
                }
            }
        }
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            primary = new TextureUnit(OpenGlHelper.defaultTexUnit, textures, !text);
            lightmap = new TextureUnit(OpenGlHelper.lightmapTexUnit, textures, true);
        }
        brightnessX = OpenGlHelper.lastBrightnessX;
        brightnessY = OpenGlHelper.lastBrightnessY;
    }

    private static float[] floats(int parameter, int count) {
        FloatBuffer values = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(parameter, values);
        float[] copied = new float[count];
        values.get(copied);
        return copied;
    }

    private static int[] integers(int parameter) {
        IntBuffer values = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(parameter, values);
        return new int[] { values.get(0), values.get(1), values.get(2), values.get(3) };
    }

    public void apply(CaptureReplayTransform transform) {
        for (int index = 0; index < enabled.length; index++) {
            if (enabled[index]) GL11.glEnable(ENABLES[index]);
            else GL11.glDisable(ENABLES[index]);
        }
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(0);
        GL11.glDepthFunc(scalars[0]);
        GL11.glCullFace(scalars[1]);
        GL11.glFrontFace(scalars[2]);
        GL11.glShadeModel(scalars[3]);
        GL11.glAlphaFunc(scalars[4], alphaReference);
        OpenGlHelper.glBlendFunc(scalars[5], scalars[6], scalars[7], scalars[8]);
        GL14.glBlendEquation(scalars[9]);
        GL11.glColorMaterial(scalars[11], scalars[12]);
        GL11.glDepthMask(depthMask);
        GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GL11.glLineWidth(lineWidth);
        GL11.glPointSize(pointSize);
        GL11.glColor4f(color[0], color[1], color[2], color[3]);
        GL11.glNormal3f(normal[0], normal[1], normal[2]);
        GL11.glFogi(GL11.GL_FOG_MODE, scalars[10]);
        GL11.glFogf(GL11.GL_FOG_DENSITY, fogDensity);
        GL11.glFogf(GL11.GL_FOG_START, fogStart);
        GL11.glFogf(GL11.GL_FOG_END, fogEnd);
        GL11.glFog(GL11.GL_FOG_COLOR, transform.values(fogColor));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        if (enabled[5]) {
            for (int index = 0; index < 8; index++) GL11.glDisable(GL11.GL_LIGHT0 + index);
            for (Light light : lights) light.apply(transform);
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, twoSided);
            GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, transform.values(lightModel));
            for (int side = 0; side < 2; side++) {
                for (int index = 0; index < MATERIAL_PARAMETERS.length; index++) {
                    GL11.glMaterial(
                        side == 0 ? GL11.GL_FRONT : GL11.GL_BACK,
                        MATERIAL_PARAMETERS[index],
                        transform.values(material[side * MATERIAL_PARAMETERS.length + index]));
                }
            }
        }
        primary.apply(transform);
        lightmap.apply(transform);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        transform.projection(projection, viewport);
        transform.load(GL11.GL_MODELVIEW, model);
        if (scissorEnabled) transform.scissor(scissor);
        else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static class TextureUnit {

        private final int unit;
        private final boolean enabled;
        private final int texture;
        private final float[] matrix;
        private final float[] coordinates;

        public TextureUnit(int unit, CaptureTextureSnapshotStore textures, boolean copy) {
            this.unit = unit;
            OpenGlHelper.setActiveTexture(unit);
            enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            texture = enabled && copy ? textures.capture(bound) : bound;
            if (enabled && GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE) != GL11.GL_MODULATE)
                throw new IllegalArgumentException("This texture requires a custom texture environment snapshot");
            matrix = floats(GL11.GL_TEXTURE_MATRIX, 16);
            coordinates = floats(GL11.GL_CURRENT_TEXTURE_COORDS, 4);
        }

        public void apply(CaptureReplayTransform transform) {
            OpenGlHelper.setActiveTexture(unit);
            if (enabled) GL11.glEnable(GL11.GL_TEXTURE_2D);
            else GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
            transform.load(GL11.GL_TEXTURE, matrix);
            if (unit == OpenGlHelper.defaultTexUnit) GL11.glTexCoord2f(coordinates[0], coordinates[1]);
            else OpenGlHelper.setLightmapTextureCoords(unit, coordinates[0], coordinates[1]);
        }
    }

    public static class Light {

        private final int light;
        private final float[][] values = new float[LIGHT_PARAMETERS.length][];

        public Light(int light) {
            this.light = GL11.GL_LIGHT0 + light;
            for (int index = 0; index < LIGHT_PARAMETERS.length; index++) {
                FloatBuffer data = BufferUtils.createFloatBuffer(4);
                GL11.glGetLight(this.light, LIGHT_PARAMETERS[index], data);
                int count = index == 4 ? 3 : index > 4 ? 1 : 4;
                values[index] = new float[count];
                data.get(values[index]);
            }
        }

        public void apply(CaptureReplayTransform transform) {
            GL11.glEnable(light);
            for (int index = 0; index < values.length; index++) {
                GL11.glLight(light, LIGHT_PARAMETERS[index], transform.values(values[index]));
            }
        }
    }
}
