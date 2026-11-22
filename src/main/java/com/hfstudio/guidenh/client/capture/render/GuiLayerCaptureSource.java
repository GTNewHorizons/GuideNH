package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.guide.render.GuiRenderVisibility;

public class GuiLayerCaptureSource implements AutoCloseable {

    private Framebuffer framebuffer;
    private CaptureRenderScope scope;
    private CaptureViewport viewport;
    private GuiRenderVisibility visibility;

    public boolean drawing() {
        return visibility != null;
    }

    public void begin(CaptureViewport current, boolean showDebug, boolean collectGeometry) {
        visibility = new GuiRenderVisibility(showDebug, collectGeometry);
        if (!OpenGlHelper.isFramebufferEnabled()) return;
        scope = new CaptureRenderScope();
        viewport = current;
        try {
            if (framebuffer == null || framebuffer.framebufferWidth != current.pixelWidth()
                || framebuffer.framebufferHeight != current.pixelHeight()) {
                if (framebuffer != null) framebuffer.deleteFramebuffer();
                framebuffer = new Framebuffer(current.pixelWidth(), current.pixelHeight(), true);
                framebuffer.setFramebufferColor(0, 0, 0, 0);
            }
            framebuffer.bindFramebuffer(true);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
            OpenGlHelper
                .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } catch (RuntimeException error) {
            restoreScope();
            throw error;
        }
    }

    public void finish(Minecraft minecraft) {
        if (visibility == null) return;
        boolean captured = scope != null;
        restoreScope();
        if (!captured) return;
        try (CaptureRenderScope composite = new CaptureRenderScope()) {
            minecraft.entityRenderer.setupOverlayRendering();
            CaptureTextureRenderer
                .draw(framebuffer.framebufferTexture, viewport.logicalWidth(), viewport.logicalHeight(), true, true);
        }
    }

    public CaptureFrame read(GlCapturePixelReader reader, long timestamp) {
        try (CaptureRenderScope reading = new CaptureRenderScope()) {
            framebuffer.bindFramebuffer(true);
            return reader
                .readPremultiplied(0, 0, framebuffer.framebufferWidth, framebuffer.framebufferHeight, timestamp);
        }
    }

    public boolean available() {
        return framebuffer != null && OpenGlHelper.isFramebufferEnabled();
    }

    private void restoreScope() {
        visibility.close();
        visibility = null;
        if (scope != null) scope.close();
        scope = null;
    }

    @Override
    public void close() {
        if (visibility != null) {
            restoreScope();
        }
        if (framebuffer != null) {
            try (CaptureRenderScope deletion = new CaptureRenderScope()) {
                framebuffer.deleteFramebuffer();
            }
            framebuffer = null;
        }
    }
}
