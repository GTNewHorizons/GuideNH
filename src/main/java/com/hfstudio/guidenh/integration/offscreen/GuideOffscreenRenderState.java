package com.hfstudio.guidenh.integration.offscreen;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class GuideOffscreenRenderState {

    private int program;
    private int matrixMode;
    private int activeTexture;
    private int drawFramebuffer;
    private int readFramebuffer;
    private int arrayBuffer;
    private int elementBuffer;

    public void capture() {
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT | GL11.GL_CLIENT_PIXEL_STORE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glPushMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glPushMatrix();
        GL13.glActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
    }

    public void restore() {
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glPopMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();
        GL20.glUseProgram(program);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        GL13.glActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
    }
}
