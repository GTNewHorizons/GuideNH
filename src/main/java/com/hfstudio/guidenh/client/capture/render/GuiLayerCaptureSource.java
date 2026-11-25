package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureLayerSource;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.guide.render.GuiRenderVisibility;

public class GuiLayerCaptureSource implements CaptureLayerSource {

    private Framebuffer framebuffer;
    private CaptureRenderScope scope;
    private CaptureViewport viewport;
    private GuiRenderVisibility visibility;
    private long completedFrame = Long.MIN_VALUE;
    private boolean includeWorld;
    private CaptureMemoryAccount memory;
    private CaptureFramebufferResources.Allocation allocation;

    @Override
    public void memoryAccount(CaptureMemoryAccount account) {
        if (framebuffer != null && account != memory)
            throw new IllegalStateException("A live GUI framebuffer cannot change its allocation budget");
        memory = account;
    }

    @Override
    public void beginGui(CaptureContext context, CaptureContentOptions content, boolean collectGeometry) {
        completedFrame = Long.MIN_VALUE;
        includeWorld = content.includeWorld();
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
        if (!includeWorld) return read(reader, timestamp);
        return CaptureWorldReader.read(context, reader, timestamp);
    }

    @Override
    public CaptureFrame captureGuiLayer(CaptureContext context, CapturePixelReader reader, long timestamp) {
        return read(reader, timestamp);
    }

    @Override
    public CaptureReadback enqueueGuiLayer(CaptureContext context, CapturePixelReader reader, long timestamp,
        long memoryBudget) {
        try (CaptureRenderScope reading = new CaptureRenderScope()) {
            framebuffer.bindFramebuffer(true);
            return reader.enqueue(
                0,
                0,
                framebuffer.framebufferWidth,
                framebuffer.framebufferHeight,
                timestamp,
                CaptureAlphaMode.PREMULTIPLIED,
                memoryBudget);
        }
    }

    @Override
    public boolean supportsRedraw() {
        return available();
    }

    public boolean drawing() {
        return visibility != null;
    }

    @Override
    public boolean drawingOnFramebuffer(int target) {
        return drawing() && framebuffer != null && target == framebuffer.framebufferObject;
    }

    public void bindCurrent(boolean viewport) {
        framebuffer.bindFramebuffer(viewport);
    }

    public void begin(CaptureViewport current, boolean showDebug, boolean collectGeometry) {
        visibility = new GuiRenderVisibility(showDebug, collectGeometry);
        if (!OpenGlHelper.isFramebufferEnabled()) return;
        viewport = current;
        try {
            scope = new CaptureRenderScope();
            if (framebuffer == null || framebuffer.framebufferWidth != current.pixelWidth()
                || framebuffer.framebufferHeight != current.pixelHeight()) {
                releaseFramebuffer();
                allocation = CaptureFramebufferResources.create(current.pixelWidth(), current.pixelHeight(), memory);
                framebuffer = allocation.framebuffer();
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
        } catch (RuntimeException | Error error) {
            CaptureCleanup cleanup = new CaptureCleanup(error);
            cleanup.run(this::restoreScope);
            throw error;
        }
    }

    public void finish(Minecraft minecraft) {
        if (visibility == null) return;
        boolean captured = scope != null;
        restoreScope();
        if (!captured) return;
        try (CaptureRenderScope composite = new CaptureRenderScope()) {
            minecraft.getFramebuffer()
                .bindFramebuffer(true);
            minecraft.entityRenderer.setupOverlayRendering();
            CaptureTextureRenderer.prepareOverlay();
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
        GuiRenderVisibility previousVisibility = visibility;
        CaptureRenderScope previousScope = scope;
        visibility = null;
        scope = null;
        CaptureCleanup cleanup = new CaptureCleanup();
        cleanup.close(previousVisibility);
        cleanup.close(previousScope);
        cleanup.rethrow();
    }

    @Override
    public void close() {
        completedFrame = Long.MIN_VALUE;
        CaptureCleanup cleanup = new CaptureCleanup();
        if (visibility != null) {
            cleanup.run(this::restoreScope);
        }
        cleanup.run(this::releaseFramebuffer);
        cleanup.rethrow();
    }

    private void releaseFramebuffer() {
        CaptureFramebufferResources.Allocation previous = allocation;
        allocation = null;
        framebuffer = null;
        if (previous != null) previous.close();
    }
}
