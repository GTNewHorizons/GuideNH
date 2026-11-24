package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

public class CaptureFramebufferResources {

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
}
