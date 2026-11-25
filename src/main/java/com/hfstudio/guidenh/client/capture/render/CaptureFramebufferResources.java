package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryReservation;

public class CaptureFramebufferResources {

    public static Allocation create(int width, int height, CaptureMemoryAccount memory) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Capture framebuffer dimensions must be positive");
        long bytes = Math.multiplyExact(Math.multiplyExact((long) width, height), 8);
        CaptureMemoryReservation reservation = new CaptureMemoryReservation(memory, bytes);
        Framebuffer framebuffer = null;
        try {
            framebuffer = new Framebuffer(width, height, true);
            return new Allocation(framebuffer, reservation);
        } catch (RuntimeException | Error failure) {
            Framebuffer failed = framebuffer;
            CaptureCleanup cleanup = new CaptureCleanup(failure);
            cleanup.run(() -> delete(failed));
            cleanup.close(reservation);
            throw failure;
        }
    }

    public static void delete(Framebuffer framebuffer) {
        if (framebuffer == null) return;
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            if (framebuffer.depthBuffer >= 0) {
                OpenGlHelper.func_153184_g(framebuffer.depthBuffer);
                framebuffer.depthBuffer = -1;
            }
            if (framebuffer.framebufferTexture >= 0) {
                TextureUtil.deleteTexture(framebuffer.framebufferTexture);
                framebuffer.framebufferTexture = -1;
            }
            if (framebuffer.framebufferObject >= 0) {
                OpenGlHelper.func_153174_h(framebuffer.framebufferObject);
                framebuffer.framebufferObject = -1;
            }
        }
    }

    public record Allocation(Framebuffer framebuffer, CaptureMemoryReservation reservation) implements AutoCloseable {

        @Override
        public void close() {
            CaptureCleanup cleanup = new CaptureCleanup();
            cleanup.run(() -> delete(framebuffer));
            cleanup.close(reservation);
            cleanup.rethrow();
        }
    }
}
