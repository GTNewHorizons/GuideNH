package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.guide.render.GuiRenderVisibility;

public class GuiLayerCaptureSource implements CaptureSource {

    private Framebuffer framebuffer;
    private CaptureRenderScope scope;
    private CaptureViewport viewport;
    private GuiRenderVisibility visibility;
    private long completedFrame = Long.MIN_VALUE;

    @Override
    public void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry) {
        completedFrame = Long.MIN_VALUE;
        begin(context.viewport(), content.includeDebug(), collectGeometry);
    }

    @Override
    public void finishGui(CaptureContext context) {
        finish(Minecraft.getMinecraft());
        if (available()) completedFrame = context.frameId();
    }

    @Override
    public boolean bindMainFramebuffer(boolean viewport) {
        if (!drawing() || !available()) return false;
        bindCurrent(viewport);
        return true;
    }

    @Override
    public CaptureProviderAvailability readiness(CaptureContext context) {
        return available() && completedFrame == context.frameId() ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("The isolated GUI frame has not completed");
    }

    @Override
    public CaptureFrame captureCompletedFrame(CaptureContext context, CapturePixelReader reader, long timestamp) {
        return read(reader, timestamp);
    }

    @Override
    public boolean supportsRedraw() {
        return available();
    }

    public boolean drawing() {
        return visibility != null;
    }

    public void bindCurrent(boolean viewport) {
        framebuffer.bindFramebuffer(viewport);
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
            CaptureTextureRenderer.draw(
                framebuffer.framebufferTexture,
                viewport.bounds()
                    .width(),
                viewport.bounds()
                    .height(),
                true,
                true);
        }
    }

    public CaptureFrame read(CapturePixelReader reader, long timestamp) {
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
        completedFrame = Long.MIN_VALUE;
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
