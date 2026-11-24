package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureDiscardOverlay implements AutoCloseable {

    private Framebuffer framebuffer;
    private CaptureRenderScope scope;

    public boolean active() {
        return scope != null;
    }

    public void begin(CaptureViewport viewport) {
        if (active()) return;
        scope = new CaptureRenderScope();
        try {
            if (framebuffer == null || framebuffer.framebufferWidth != viewport.pixelWidth()
                || framebuffer.framebufferHeight != viewport.pixelHeight()) {
                CaptureFramebufferResources.delete(framebuffer);
                framebuffer = new Framebuffer(viewport.pixelWidth(), viewport.pixelHeight(), true);
            }
            bind(true);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        } catch (RuntimeException error) {
            end();
            throw error;
        }
    }

    public void bind(boolean viewport) {
        framebuffer.bindFramebuffer(viewport);
    }

    public void end() {
        if (scope == null) return;
        scope.close();
        scope = null;
    }

    @Override
    public void close() {
        end();
        CaptureFramebufferResources.delete(framebuffer);
        framebuffer = null;
    }
}
