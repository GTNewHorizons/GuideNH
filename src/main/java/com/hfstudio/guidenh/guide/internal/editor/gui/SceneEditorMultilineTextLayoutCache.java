package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.github.bsideup.jabel.Desugar;

import lombok.Getter;

public class SceneEditorMultilineTextLayoutCache {

    /**
     * How much of a logical line is handed to the font at once when wrapping. Measuring a window instead of
     * the whole remainder costs a constant copy per wrapped chunk rather than one that shrinks with it, and
     * a window this wide always contains more than one wrapped chunk, so the wrap points do not change.
     */
    private static final int MEASURE_WINDOW_CHARS = 4096;

    private final List<VisualLine> visualLines = new ArrayList<>();
    private List<VisualLine> readonlyVisualLines = List.of();
    @Getter
    private int contentWidthPixels;
    @Getter
    private int contentHeightPixels;

    public void rebuild(String text, FontRenderer fontRenderer, int textWidth, boolean wrapEnabled, int lineHeight) {
        visualLines.clear();
        contentWidthPixels = 0;
        contentHeightPixels = 0;

        String safeText = text != null ? text : "";
        int safeTextWidth = Math.max(1, textWidth);
        int lineStart = 0;
        for (int i = 0; i <= safeText.length(); i++) {
            boolean atEnd = i == safeText.length();
            if (!atEnd && safeText.charAt(i) != '\n') {
                continue;
            }

            String logicalLine = safeText.substring(lineStart, i);
            boolean endsWithNewline = !atEnd;
            appendLogicalLine(logicalLine, lineStart, endsWithNewline, fontRenderer, safeTextWidth, wrapEnabled);
            lineStart = i + 1;
        }

        if (visualLines.isEmpty()) {
            visualLines.add(new VisualLine(0, 0, "", false));
        }

        readonlyVisualLines = List.copyOf(new ArrayList<>(visualLines));
        contentHeightPixels = visualLines.size() * Math.max(0, lineHeight);
    }

    public List<VisualLine> getVisualLines() {
        return readonlyVisualLines;
    }

    private void appendLogicalLine(String logicalLine, int lineStart, boolean endsWithNewline,
        FontRenderer fontRenderer, int textWidth, boolean wrapEnabled) {
        if (!wrapEnabled) {
            visualLines.add(new VisualLine(lineStart, lineStart + logicalLine.length(), logicalLine, endsWithNewline));
            contentWidthPixels = Math.max(contentWidthPixels, fontRenderer.getStringWidth(logicalLine));
            return;
        }

        if (logicalLine.isEmpty()) {
            visualLines.add(new VisualLine(lineStart, lineStart, "", endsWithNewline));
            return;
        }

        int offset = 0;
        while (offset < logicalLine.length()) {
            // Measuring a window instead of everything that is left bounds the copy per chunk, and the
            // window is widened until the font stops at the width rather than at the end of the window: a
            // window the font consumed entirely could hide text that still fits, which happens when a run
            // of zero-width formatting codes makes one chunk longer than the window.
            int windowEnd = Math.min(logicalLine.length(), offset + MEASURE_WINDOW_CHARS);
            String chunk;
            while (true) {
                String window = logicalLine.substring(offset, windowEnd);
                chunk = fontRenderer.trimStringToWidth(window, textWidth);
                if (chunk.length() < window.length() || windowEnd >= logicalLine.length()) {
                    break;
                }
                windowEnd = Math.min(logicalLine.length(), windowEnd + MEASURE_WINDOW_CHARS);
            }
            if (chunk.isEmpty()) {
                chunk = logicalLine.substring(offset, offset + 1);
            }
            int consumed = Math.max(1, chunk.length());
            int startIndex = lineStart + offset;
            int endIndex = startIndex + consumed;
            boolean lineBreak = endsWithNewline && endIndex == lineStart + logicalLine.length();
            visualLines.add(new VisualLine(startIndex, endIndex, chunk, lineBreak));
            contentWidthPixels = Math.max(contentWidthPixels, fontRenderer.getStringWidth(chunk));
            offset += consumed;
        }
    }

    @Desugar
    public record VisualLine(int startIndex, int endIndex, String text, boolean endsWithNewline) {}
}
