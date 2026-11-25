package com.hfstudio.guidenh.client.capture.render;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;

public class CaptureTextureSnapshotStore implements AutoCloseable {

    private final ContextCapabilities context = GLContext.getCapabilities();
    private final Map<Integer, TextureCopy> textures = new LinkedHashMap<>();
    private final LongSupplier budget;
    private final CaptureMemoryBudget memory;
    private final Thread owner = Thread.currentThread();
    private long bytes;
    private int framebuffer;
    private boolean closed;

    public CaptureTextureSnapshotStore(long budget) {
        this(() -> budget);
        if (budget < 0) throw new IllegalArgumentException("The texture snapshot budget cannot be negative");
    }

    public CaptureTextureSnapshotStore(LongSupplier budget) {
        this(budget, null);
    }

    public CaptureTextureSnapshotStore(LongSupplier budget, CaptureMemoryBudget memory) {
        this.budget = Objects.requireNonNull(budget);
        this.memory = memory;
    }

    public long bytes() {
        return bytes;
    }

    public int capture(int original) {
        checkThread();
        if (closed) throw new IllegalStateException("The texture snapshot store is closed");
        if (!currentContext()) throw new IllegalStateException("The texture snapshot context changed");
        if (original == 0) return 0;
        TextureCopy existing = textures.get(original);
        if (existing != null) return existing.texture();
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            if (GLContext.getCapabilities().OpenGL21) GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, original);
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("The GUI texture has no readable image");
            int min = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            int mag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            int wrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
            int wrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
            int maximum = Math.min(31, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL));
            long required = 0;
            int levels = 0;
            for (int level = 0; level <= maximum; level++) {
                int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_WIDTH);
                int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_HEIGHT);
                if (w == 0 || h == 0) break;
                required = Math.addExact(required, new CaptureOutputSize(w, h).rgbaBytes());
                levels++;
            }
            long available = budget.getAsLong();
            if (available < bytes || required > available - bytes)
                throw new IllegalArgumentException("Immutable GUI textures exceed the snapshot memory budget");
            if (memory != null) memory.reserve(required);
            int copy = 0;
            try {
                if (framebuffer == 0) framebuffer = OpenGlHelper.func_153165_e();
                copy = GL11.glGenTextures();
                if (copy == 0) throw new IllegalStateException("The GUI texture snapshot could not be allocated");
                OpenGlHelper.func_153171_g(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
                GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, copy);
                for (int level = 0; level < levels; level++) {
                    int w = Math.max(1, width >> level);
                    int h = Math.max(1, height >> level);
                    GL11.glTexImage2D(
                        GL11.GL_TEXTURE_2D,
                        level,
                        GL11.GL_RGBA8,
                        w,
                        h,
                        0,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        (ByteBuffer) null);
                    OpenGlHelper.func_153188_a(
                        EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                        GL11.GL_TEXTURE_2D,
                        original,
                        level);
                    if (OpenGlHelper.func_153167_i(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
                        != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT)
                        throw new IllegalArgumentException(
                            "This GUI texture cannot be copied into an immutable snapshot");
                    GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, level, 0, 0, 0, 0, w, h);
                }
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, levels - 1);
                textures.put(original, new TextureCopy(copy, required));
                bytes += required;
                return copy;
            } catch (RuntimeException | Error failure) {
                try {
                    if (copy != 0) GL11.glDeleteTextures(copy);
                } finally {
                    if (memory != null) memory.release(required);
                }
                throw failure;
            }
        }
    }

    @Override
    public void close() {
        checkThread();
        if (closed) return;
        if (currentContext()) {
            for (TextureCopy texture : textures.values()) GL11.glDeleteTextures(texture.texture());
            if (framebuffer != 0) OpenGlHelper.func_153174_h(framebuffer);
        }
        framebuffer = 0;
        textures.clear();
        if (memory != null) memory.release(bytes);
        bytes = 0;
        closed = true;
    }

    private void checkThread() {
        if (Thread.currentThread() != owner)
            throw new IllegalStateException("Texture snapshots belong to their creating render thread");
    }

    private boolean currentContext() {
        try {
            return GLContext.getCapabilities() == context;
        } catch (IllegalStateException unavailable) {
            return false;
        }
    }

    public record TextureCopy(int texture, long bytes) {}
}
