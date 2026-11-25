package com.hfstudio.guidenh.client.capture.render;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureDrawRecorder implements AutoCloseable {

    private final CaptureMemoryBudget memory = new CaptureMemoryBudget(0);
    private CaptureRecordedSnapshot recording;
    private int textDepth;
    private String failure = "";

    public void begin(CaptureContext context, CaptureContentOptions content, long budget) {
        close();
        failure = "";
        memory.limit(budget);
        recording = new CaptureRecordedSnapshot(context, content, budget, memory);
    }

    public void mesh(int[] raw, int count, int used, int mode, boolean texture, boolean color, boolean brightness,
        boolean normals) {
        if (recording == null || textDepth != 0 || count <= 0) return;
        try {
            recording.reserve((long) used * 4 + 2048);
            recording.add(
                new CaptureMeshReplayCommand(
                    recording.textures(),
                    raw,
                    count,
                    used,
                    mode,
                    texture,
                    color,
                    brightness,
                    normals));
        } catch (RuntimeException unsupported) {
            failed(unsupported);
        }
    }

    public void beginText(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        beginText(font, text, x, y, color, shadow, true);
    }

    public void beginText(FontRenderer font, String text, int x, int y, int color, boolean shadow, boolean collect) {
        textDepth++;
        if (!collect || recording == null || textDepth != 1 || text == null || text.isEmpty()) return;
        try {
            recording.reserve((long) text.length() * 2 + 2048);
            recording.add(new CaptureTextReplayCommand(recording.textures(), font, text, x, y, color, shadow));
        } catch (RuntimeException unsupported) {
            failed(unsupported);
        }
    }

    public void endText() {
        if (textDepth > 0) textDepth--;
    }

    public void mask(List<CaptureRect> masks, CaptureViewport viewport) {
        if (recording == null || masks.isEmpty()) return;
        try {
            recording.reserve((long) masks.size() * 48);
            recording.add(new CaptureMaskReplayCommand(masks, viewport));
        } catch (RuntimeException unsupported) {
            failed(unsupported);
        }
    }

    private void failed(RuntimeException error) {
        failure = error.getMessage();
        if (recording != null) recording.close();
        recording = null;
    }

    public String failure() {
        return failure;
    }

    public CaptureRecordedSnapshot take() {
        CaptureRecordedSnapshot snapshot = recording;
        recording = null;
        return snapshot == null ? null : snapshot.seal();
    }

    @Override
    public void close() {
        if (recording != null) recording.close();
        recording = null;
        textDepth = 0;
    }
}
