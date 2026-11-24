package com.hfstudio.guidenh.integration.angelica.capture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;

public class AngelicaCaptureReadbackScope implements AutoCloseable {

    private static final int[] PARAMETERS = { GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_SKIP_PIXELS,
        GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SWAP_BYTES };
    private final RenderBackend backend;
    private final int[] values = new int[PARAMETERS.length];
    private final int packBuffer;
    private boolean closed;

    public AngelicaCaptureReadbackScope(RenderBackend backend) {
        this.backend = backend;
        packBuffer = backend.getInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        for (int index = 0; index < PARAMETERS.length; index++) values[index] = backend.getInteger(PARAMETERS[index]);
        try {
            backend.bindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            for (int index = 0; index < PARAMETERS.length; index++)
                backend.pixelStorei(PARAMETERS[index], index == 0 ? 1 : 0);
        } catch (RuntimeException error) {
            try {
                close();
            } catch (RuntimeException restoration) {
                error.addSuppressed(restoration);
            }
            throw error;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (int index = PARAMETERS.length - 1; index >= 0; index--)
            backend.pixelStorei(PARAMETERS[index], values[index]);
        backend.bindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
    }
}
