package com.hfstudio.guidenh.guide.render;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

public class GuideIconCapture implements AutoCloseable {

    private Framebuffer framebuffer;
    private ByteBuffer pixelBuffer;
    private int size;

    public BufferedImage capture(int edge, Runnable draw) {
        if (edge < 1 || edge > 512) throw new IllegalArgumentException("Icon edge must be between 1 and 512");
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!minecraft.func_152345_ab()) throw new IllegalStateException("Icon capture requires the client thread");
        int previousWidth = minecraft.displayWidth;
        int previousHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousPackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean previousLightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(previousTextureUnit);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT | GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL20.glUseProgram(0);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            ensureTarget(edge);
            minecraft.displayWidth = edge;
            minecraft.displayHeight = edge;
            minecraft.gameSettings.guiScale = 1;
            framebuffer.bindFramebuffer(true);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0, edge, edge, 0, 1000, 3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            modelViewPushed = true;
            GL11.glLoadIdentity();
            GL11.glTranslatef(0, 0, -2000);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            draw.run();
            return readPixels();
        } finally {
            if (modelViewPushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            }
            minecraft.displayWidth = previousWidth;
            minecraft.displayHeight = previousHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            OpenGlHelper.func_153171_g(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            OpenGlHelper.func_153171_g(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousPackBuffer);
            GL20.glUseProgram(previousProgram);
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            if (previousLightmap) GL11.glEnable(GL11.GL_TEXTURE_2D);
            else GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(previousTextureUnit);
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private void ensureTarget(int edge) {
        if (framebuffer != null && size == edge) return;
        close();
        size = edge;
        framebuffer = new Framebuffer(edge, edge, true);
        framebuffer.setFramebufferColor(0, 0, 0, 0);
        pixelBuffer = BufferUtils.createByteBuffer(edge * edge * 4);
    }

    private BufferedImage readPixels() {
        pixelBuffer.clear();
        GL11.glReadPixels(0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int index = (x + y * size) * 4;
                pixels[x + (size - 1 - y) * size] = (pixelBuffer.get(index + 3) & 255) << 24
                    | (pixelBuffer.get(index) & 255) << 16
                    | (pixelBuffer.get(index + 1) & 255) << 8
                    | pixelBuffer.get(index + 2) & 255;
            }
        }
        return image;
    }

    @Override
    public void close() {
        if (framebuffer != null) framebuffer.deleteFramebuffer();
        framebuffer = null;
        pixelBuffer = null;
        size = 0;
    }
}
