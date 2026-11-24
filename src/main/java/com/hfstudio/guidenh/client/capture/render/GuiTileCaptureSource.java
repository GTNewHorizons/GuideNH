package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.guide.render.GuiRenderVisibility;

public class GuiTileCaptureSource implements AutoCloseable {

    public static final int GUARD = 2;
    private Framebuffer framebuffer;
    private boolean drawing;

    public boolean drawing() {
        return drawing;
    }

    public void bindCurrent(boolean viewport) {
        framebuffer.bindFramebuffer(viewport);
    }

    public int tileLimit(int requested, long memoryBudget) {
        IntBuffer dimensions = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS, dimensions);
        int limit = Math.min(
            GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE),
            GL11.glGetInteger(EXTFramebufferObject.GL_MAX_RENDERBUFFER_SIZE_EXT));
        limit = Math.min(limit, Math.min(dimensions.get(0), dimensions.get(1)));
        limit = Math.min(limit, (int) Math.sqrt(memoryBudget / 24));
        return Math.min(requested, limit - GUARD * 2);
    }

    public CaptureFrame render(CaptureTile tile, CaptureRenderSnapshot snapshot, CapturePixelReader reader,
        boolean includeDebug, long timestamp) {
        int width = tile.width() + GUARD * 2;
        int height = tile.height() + GUARD * 2;
        CaptureRect logical = tile.logicalBounds();
        double pixelX = logical.width() / tile.width();
        double pixelY = logical.height() / tile.height();
        try (CaptureRenderScope scope = new CaptureRenderScope();
            GuiRenderVisibility visibility = new GuiRenderVisibility(includeDebug)) {
            if (framebuffer == null || framebuffer.framebufferWidth != width
                || framebuffer.framebufferHeight != height) {
                CaptureFramebufferResources.delete(framebuffer);
                framebuffer = new Framebuffer(width, height, true);
                framebuffer.setFramebufferColor(0, 0, 0, 0);
            }
            framebuffer.bindFramebuffer(true);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(
                logical.x() - GUARD * pixelX,
                logical.right() + GUARD * pixelX,
                logical.bottom() + GUARD * pixelY,
                logical.y() - GUARD * pixelY,
                1000,
                3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(0, 0, -2000);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glColor4f(1, 1, 1, 1);
            OpenGlHelper
                .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawing = true;
            try {
                snapshot.draw();
                return reader.readPremultiplied(0, 0, width, height, timestamp);
            } finally {
                drawing = false;
            }
        }
    }

    @Override
    public void close() {
        if (framebuffer == null) return;
        CaptureFramebufferResources.delete(framebuffer);
        framebuffer = null;
    }
}
