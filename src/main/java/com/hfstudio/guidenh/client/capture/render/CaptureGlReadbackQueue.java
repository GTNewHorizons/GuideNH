package com.hfstudio.guidenh.client.capture.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;

public class CaptureGlReadbackQueue implements AutoCloseable {

    private static final int MAX_READBACKS = 3;
    private final Thread owner = Thread.currentThread();
    private final ContextCapabilities capabilities = GLContext.getCapabilities();
    private final boolean coreSync = capabilities.OpenGL32;
    private final List<Slot> slots = new ArrayList<>(MAX_READBACKS);
    private long allocatedBytes;
    private boolean closed;

    public static boolean supported() {
        return GLContext.getCapabilities().OpenGL21
            && (GLContext.getCapabilities().OpenGL32 || GLContext.getCapabilities().GL_ARB_sync);
    }

    private void checkThread() {
        if (Thread.currentThread() != owner)
            throw new IllegalStateException("Pixel readbacks belong to their submitting rendering thread");
    }

    private boolean currentContext() {
        try {
            return GLContext.getCapabilities() == capabilities;
        } catch (IllegalStateException unavailable) {
            return false;
        }
    }

    public CaptureReadback submit(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alphaMode,
        long memoryBudget) {
        checkThread();
        if (closed) throw new IllegalStateException("The pixel readback queue is closed");
        if (!currentContext()) throw new IllegalStateException("The GPU readback context changed");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Capture dimensions must be positive");
        int bytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (bytes > memoryBudget) throw new IllegalArgumentException("The GPU readback exceeds the memory budget");
        Slot slot = acquire(bytes, memoryBudget);
        slot.leased = true;
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, slot.buffer);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_FALSE);
            GL11.glReadPixels(x, y, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, 0L);
            slot.fence = coreSync ? GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
                : ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            if (slot.fence == null) throw new IllegalStateException("The GPU could not create a readback fence");
            GL11.glFlush();
            return new Readback(slot, width, height, timestamp, alphaMode);
        } catch (RuntimeException error) {
            release(slot);
            throw error;
        }
    }

    private Slot acquire(int bytes, long memoryBudget) {
        Slot selected = slots.stream()
            .filter(slot -> !slot.leased && slot.bytes == bytes)
            .findFirst()
            .orElse(null);
        if (selected == null) selected = slots.stream()
            .filter(slot -> !slot.leased)
            .findFirst()
            .orElse(null);
        if (selected == null) {
            if (slots.size() == MAX_READBACKS) throw new IllegalStateException("The GPU readback queue is full");
            selected = new Slot();
            slots.add(selected);
        }
        for (Slot slot : slots) {
            if (allocatedBytes - selected.bytes + bytes <= memoryBudget) break;
            if (slot != selected && !slot.leased) delete(slot);
        }
        if (allocatedBytes - selected.bytes + bytes > memoryBudget)
            throw new IllegalStateException("Pending GPU readbacks consume the available memory budget");
        if (selected.bytes != bytes) {
            delete(selected);
            try (CaptureRenderScope scope = new CaptureRenderScope()) {
                selected.buffer = GL15.glGenBuffers();
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, selected.buffer);
                GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, bytes, GL15.GL_STREAM_READ);
                selected.bytes = bytes;
                allocatedBytes += bytes;
            }
        }
        return selected;
    }

    private void release(Slot slot) {
        checkThread();
        if (slot.fence != null) {
            if (currentContext()) {
                if (coreSync) GL32.glDeleteSync(slot.fence);
                else ARBSync.glDeleteSync(slot.fence);
            }
            slot.fence = null;
        }
        slot.leased = false;
    }

    private void delete(Slot slot) {
        release(slot);
        if (slot.buffer != 0 && currentContext()) GL15.glDeleteBuffers(slot.buffer);
        allocatedBytes -= slot.bytes;
        slot.buffer = 0;
        slot.bytes = 0;
        slot.mapping = null;
    }

    @Override
    public void close() {
        checkThread();
        if (closed) return;
        for (Slot slot : slots) delete(slot);
        slots.clear();
        closed = true;
    }

    public static class Slot {

        private int buffer;
        private int bytes;
        private GLSync fence;
        private ByteBuffer mapping;
        private boolean leased;
    }

    public class Readback implements CaptureReadback {

        private final Slot slot;
        private final int width;
        private final int height;
        private final long timestamp;
        private final CaptureAlphaMode alphaMode;
        private boolean released;

        private Readback(Slot slot, int width, int height, long timestamp, CaptureAlphaMode alphaMode) {
            this.slot = slot;
            this.width = width;
            this.height = height;
            this.timestamp = timestamp;
            this.alphaMode = alphaMode;
        }

        @Override
        public boolean ready() {
            checkThread();
            if (released || closed) throw new IllegalStateException("The GPU readback is closed");
            if (!currentContext()) throw new IllegalStateException("The GPU readback context changed");
            int status = coreSync ? GL32.glClientWaitSync(slot.fence, 0, 0)
                : ARBSync.glClientWaitSync(slot.fence, 0, 0);
            if (status == GL32.GL_WAIT_FAILED) throw new IllegalStateException("The GPU readback fence failed");
            return status == GL32.GL_ALREADY_SIGNALED || status == GL32.GL_CONDITION_SATISFIED;
        }

        @Override
        public CaptureFrame take() {
            if (!ready()) throw new IllegalStateException("The GPU readback is not ready");
            try (CaptureRenderScope scope = new CaptureRenderScope()) {
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, slot.buffer);
                slot.mapping = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, slot.bytes, slot.mapping);
                if (slot.mapping == null) throw new IllegalStateException("The GPU readback buffer could not map");
                int[] pixels = new int[Math.multiplyExact(width, height)];
                try {
                    IntBuffer words = slot.mapping.order(ByteOrder.nativeOrder())
                        .asIntBuffer();
                    for (int row = 0; row < height; row++) {
                        words.position((height - 1 - row) * width);
                        words.get(pixels, row * width, width);
                    }
                } finally {
                    if (!GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER))
                        throw new IllegalStateException("The GPU readback buffer was corrupted");
                }
                CapturePixelConverter.normalize(pixels, alphaMode);
                return new CaptureFrame(width, height, timestamp, pixels);
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            checkThread();
            if (released) return;
            if (!closed) release(slot);
            released = true;
        }
    }
}
