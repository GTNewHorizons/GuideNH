package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureWorldReader {

    private static final int STRIP_BYTES = 1 << 20;

    private CaptureWorldReader() {
    }

    public static CaptureFrame read(CaptureContext context, CapturePixelReader reader, long timestamp) {
        try (CaptureRenderScope reading = new CaptureRenderScope()) {
            // Read the world target independently of display presentation and selection overlays.
            Minecraft.getMinecraft()
                    .getFramebuffer()
                    .bindFramebuffer(true);
            GL11.glReadBuffer(OpenGlHelper.field_153200_g);
            CaptureViewport viewport = context.viewport();
            return reader.readOpaque(0, 0, viewport.pixelWidth(), viewport.pixelHeight(), timestamp);
        }
    }

    public static CaptureWorldSnapshot snapshot(CaptureContext context, CapturePixelReader reader, long timestamp) {
        CaptureViewport viewport = context.viewport();
        int width = viewport.pixelWidth();
        int height = viewport.pixelHeight();
        int[] pixels = new int[Math.multiplyExact(width, height)];
        int rowsPerStrip = (int) Math.max(1, STRIP_BYTES / ((long) width * 4));
        try (CaptureRenderScope reading = new CaptureRenderScope()) {
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
            GL11.glReadBuffer(OpenGlHelper.field_153200_g);
            for (int top = 0; top < height; top += rowsPerStrip) {
                int rows = Math.min(rowsPerStrip, height - top);
                try (CaptureFrame strip = reader.readOpaque(0, height - top - rows, width, rows, timestamp)) {
                    for (int row = 0; row < rows; row++)
                        strip.copyRow(row, 0, width, pixels, (top + row) * width);
                }
            }
        }
        return new CaptureWorldSnapshot(width, height, pixels);
    }
}
