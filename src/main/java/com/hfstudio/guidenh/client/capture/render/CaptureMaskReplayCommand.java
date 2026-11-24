package com.hfstudio.guidenh.client.capture.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureMaskReplayCommand implements CaptureReplayCommand {

    private final List<CaptureRect> pixels;
    private final int height;

    public CaptureMaskReplayCommand(List<CaptureRect> masks, CaptureViewport viewport) {
        pixels = masks.stream()
            .map(viewport::pixels)
            .toList();
        height = viewport.pixelHeight();
    }

    @Override
    public void draw(CaptureReplayTransform transform) {
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(0, 0, 0, 0);
            for (CaptureRect rectangle : pixels) {
                transform.scissor(
                    new int[] { (int) rectangle.x(), height - (int) rectangle.bottom(), (int) rectangle.width(),
                        (int) rectangle.height() });
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            }
        }
    }
}
