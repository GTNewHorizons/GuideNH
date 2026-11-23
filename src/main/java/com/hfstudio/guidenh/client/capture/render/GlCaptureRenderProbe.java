package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureCapabilities;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderProbe;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderState;

public class GlCaptureRenderProbe implements CaptureRenderProbe {

    private IntBuffer vector;
    private Object capabilitiesKey;
    private CaptureCapabilities capabilities;

    protected String backendId() {
        return "native_opengl";
    }

    protected boolean openGl() {
        return true;
    }

    protected Object contextKey() {
        return GLContext.getCapabilities();
    }

    protected int integer(int parameter) {
        return GL11.glGetInteger(parameter);
    }

    protected void integers(int parameter, IntBuffer target) {
        GL11.glGetInteger(parameter, target);
    }

    private CaptureRect rectangle(int parameter) {
        if (vector == null) vector = BufferUtils.createIntBuffer(16);
        vector.clear();
        integers(parameter, vector);
        return new CaptureRect(vector.get(0), vector.get(1), Math.max(0, vector.get(2)), Math.max(0, vector.get(3)));
    }

    protected CaptureCapabilities capabilities(long budget) {
        Object key = contextKey();
        if (capabilities == null || !Objects.equals(capabilitiesKey, key)) {
            Set<String> features = new HashSet<>();
            List<String> limitations = new ArrayList<>();
            describeCapabilities(features, limitations);
            if (vector == null) vector = BufferUtils.createIntBuffer(16);
            vector.clear();
            integers(GL11.GL_MAX_VIEWPORT_DIMS, vector);
            int width = vector.get(0);
            int height = vector.get(1);
            capabilities = new CaptureCapabilities(
                backendId(),
                features,
                integer(GL11.GL_MAX_TEXTURE_SIZE),
                OpenGlHelper.framebufferSupported ? integer(EXTFramebufferObject.GL_MAX_RENDERBUFFER_SIZE_EXT) : 0,
                width,
                height,
                budget,
                limitations);
            capabilitiesKey = key;
        } else if (capabilities.memoryBudgetBytes() != budget) {
            capabilities = new CaptureCapabilities(
                capabilities.backendId(),
                capabilities.features(),
                capabilities.maxTextureSize(),
                capabilities.maxRenderbufferSize(),
                capabilities.maxViewportWidth(),
                capabilities.maxViewportHeight(),
                budget,
                capabilities.limitations());
        }
        return capabilities;
    }

    protected void describeCapabilities(Set<String> features, List<String> limitations) {
        if (!openGl()) {
            limitations.add("GUI isolation and tiled redraw require a backend source adapter");
            return;
        }
        features.add("native_readback");
        if (OpenGlHelper.isFramebufferEnabled()) features.add("gui_framebuffer");
        if (GLContext.getCapabilities().OpenGL21) features.add("hardware_pbo");
        if (GLContext.getCapabilities().OpenGL32) features.add("hardware_fence");
    }

    @Override
    public CaptureRenderState capture(CaptureContext context, long budget) {
        boolean framebuffer = OpenGlHelper.framebufferSupported;
        int draw = framebuffer ? integer(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT) : 0;
        int read = framebuffer && (!openGl() || GLContext.getCapabilities().OpenGL30)
            ? integer(GL30.GL_READ_FRAMEBUFFER_BINDING)
            : draw;
        return new CaptureRenderState(
            backendId(),
            openGl(),
            Minecraft.getMinecraft().theWorld != null,
            shadowPass(),
            draw,
            read,
            integer(GL11.GL_DRAW_BUFFER),
            integer(GL11.GL_READ_BUFFER),
            rectangle(GL11.GL_VIEWPORT),
            integer(GL11.GL_SCISSOR_TEST) != 0,
            rectangle(GL11.GL_SCISSOR_BOX),
            capabilities(budget));
    }

    @Override
    public boolean shadowPass() {
        return false;
    }

    @Override
    public void reset() {
        vector = null;
        capabilitiesKey = null;
        capabilities = null;
    }
}
