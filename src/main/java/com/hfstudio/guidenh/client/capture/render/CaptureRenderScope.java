package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

public class CaptureRenderScope implements AutoCloseable {

    private final int matrixMode;
    private final int framebuffer;
    private final int readFramebuffer;
    private final int program;
    private final int packBuffer;
    private final int unpackBuffer;
    private final boolean separateFramebuffers;
    private final boolean multitexture;
    private final int activeTexture;
    private final int[] textureUnits;
    private final float brightnessX;
    private final float brightnessY;
    private boolean closed;

    public CaptureRenderScope() {
        separateFramebuffers = GLContext.getCapabilities().OpenGL30;
        multitexture = GLContext.getCapabilities().OpenGL13 || GLContext.getCapabilities().GL_ARB_multitexture;
        activeTexture = multitexture ? GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) : OpenGlHelper.defaultTexUnit;
        textureUnits = !multitexture ? new int[] { activeTexture }
            : activeTexture == OpenGlHelper.defaultTexUnit || activeTexture == OpenGlHelper.lightmapTexUnit
                ? new int[] { OpenGlHelper.defaultTexUnit, OpenGlHelper.lightmapTexUnit }
                : new int[] { OpenGlHelper.defaultTexUnit, OpenGlHelper.lightmapTexUnit, activeTexture };
        brightnessX = OpenGlHelper.lastBrightnessX;
        brightnessY = OpenGlHelper.lastBrightnessY;
        framebuffer = OpenGlHelper.framebufferSupported
            ? GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)
            : 0;
        readFramebuffer = separateFramebuffers ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : framebuffer;
        program = GLContext.getCapabilities().OpenGL20 ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : -1;
        packBuffer = GLContext.getCapabilities().OpenGL21 ? GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING) : -1;
        unpackBuffer = GLContext.getCapabilities().OpenGL21 ? GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING)
            : -1;
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT | GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        pushMatrix(GL11.GL_PROJECTION);
        pushMatrix(GL11.GL_MODELVIEW);
        for (int unit : textureUnits) {
            activate(unit);
            pushMatrix(GL11.GL_TEXTURE);
        }
        activate(activeTexture);
        GL11.glMatrixMode(matrixMode);
    }

    private static void pushMatrix(int mode) {
        GL11.glMatrixMode(mode);
        GL11.glPushMatrix();
    }

    private static void popMatrix(int mode) {
        GL11.glMatrixMode(mode);
        GL11.glPopMatrix();
    }

    private void activate(int unit) {
        if (multitexture) OpenGlHelper.setActiveTexture(unit);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (int index = textureUnits.length - 1; index >= 0; index--) {
            activate(textureUnits[index]);
            popMatrix(GL11.GL_TEXTURE);
        }
        activate(activeTexture);
        popMatrix(GL11.GL_MODELVIEW);
        popMatrix(GL11.GL_PROJECTION);
        GL11.glMatrixMode(matrixMode);
        if (program >= 0) GL20.glUseProgram(program);
        if (packBuffer >= 0) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }
        if (OpenGlHelper.framebufferSupported) {
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffer);
            if (separateFramebuffers) GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();
        OpenGlHelper.lastBrightnessX = brightnessX;
        OpenGlHelper.lastBrightnessY = brightnessY;
    }
}
