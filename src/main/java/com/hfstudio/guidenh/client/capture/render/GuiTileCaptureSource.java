package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;
import java.util.function.Supplier;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.guide.render.GuiRenderVisibility;

public class GuiTileCaptureSource implements AutoCloseable {

    public static final int GUARD = 2;
    private Framebuffer framebuffer;
    private boolean drawing;
    private final CaptureMemoryAccount memory;
    private CaptureFramebufferResources.Allocation allocation;

    public GuiTileCaptureSource() {
        this(null);
    }

    public GuiTileCaptureSource(CaptureMemoryAccount memory) {
        this.memory = memory;
    }

    public boolean drawing() {
        return drawing;
    }

    public void bindCurrent(boolean viewport) {
        framebuffer.bindFramebuffer(viewport);
    }

    public int tileLimit(int requested, long memoryBudget) {
        if (requested < 1 || memoryBudget < 1)
            throw new IllegalArgumentException("Capture tile dimensions and memory allowance must be positive");
        IntBuffer dimensions = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS, dimensions);
        int limit = Math.min(
            GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE),
            GL11.glGetInteger(EXTFramebufferObject.GL_MAX_RENDERBUFFER_SIZE_EXT));
        limit = Math.min(limit, Math.min(dimensions.get(0), dimensions.get(1)));
        limit = Math.min(limit, (int) Math.sqrt(memoryBudget / 24));
        if (limit <= GUARD * 2)
            throw new IllegalArgumentException("The capture memory allowance cannot fit a tile and its guard pixels");
        return Math.min(requested, limit - GUARD * 2);
    }

    public CaptureFrame render(CaptureTile tile, CaptureRenderSnapshot snapshot, CapturePixelReader reader,
        boolean includeDebug, long timestamp) {
        return draw(
            tile,
            snapshot,
            includeDebug,
            () -> reader.readPremultiplied(0, 0, tile.width() + GUARD * 2, tile.height() + GUARD * 2, timestamp));
    }

    public CaptureReadback enqueue(CaptureTile tile, CaptureRenderSnapshot snapshot, CapturePixelReader reader,
        boolean includeDebug, long timestamp, long memoryBudget) {
        return draw(
            tile,
            snapshot,
            includeDebug,
            () -> reader.enqueue(
                0,
                0,
                tile.width() + GUARD * 2,
                tile.height() + GUARD * 2,
                timestamp,
                CaptureAlphaMode.PREMULTIPLIED,
                memoryBudget));
    }

    private <T> T draw(CaptureTile tile, CaptureRenderSnapshot snapshot, boolean includeDebug, Supplier<T> readback) {
        if (tile.width() < 1 || tile.height() < 1)
            throw new IllegalArgumentException("Capture tile dimensions must be positive");
        int width = Math.addExact(tile.width(), GUARD * 2);
        int height = Math.addExact(tile.height(), GUARD * 2);
        CaptureRect logical = tile.logicalBounds();
        double pixelX = logical.width() / tile.width();
        double pixelY = logical.height() / tile.height();
        try (CaptureRenderScope scope = new CaptureRenderScope();
            GuiRenderVisibility visibility = new GuiRenderVisibility(includeDebug)) {
            if (framebuffer == null || framebuffer.framebufferWidth != width
                || framebuffer.framebufferHeight != height) {
                close();
                allocation = CaptureFramebufferResources.create(width, height, memory);
                framebuffer = allocation.framebuffer();
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
                return readback.get();
            } finally {
                drawing = false;
            }
        }
    }

    @Override
    public void close() {
        CaptureFramebufferResources.Allocation previous = allocation;
        allocation = null;
        framebuffer = null;
        if (previous != null) previous.close();
    }
}
