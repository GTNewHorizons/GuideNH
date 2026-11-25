package com.hfstudio.guidenh.client.capture.export;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureSpriteLayout;

public record CaptureSpritePageLayout(int columns, int rows, int width, int strideWidth, int strideHeight, int padding,
    int offsetX, int offsetY) {

    public static CaptureSpritePageLayout create(CaptureOutputSize frame, CaptureRecordingOptions options) {
        int padding = options.spritePadding();
        if (padding < 0 || options.spriteColumns() < 1 || options.spritePageSize() < 1)
            throw new IllegalArgumentException(
                "Sprite columns and page size must be positive, and padding nonnegative");
        CaptureSpriteLayout layout = options.spriteLayout();
        CaptureOutputSize cell = layout.resolve(frame);
        int strideWidth = Math.addExact(cell.width(), padding);
        int strideHeight = Math.addExact(cell.height(), padding);
        int columns = Math.min(options.spriteColumns(), (options.spritePageSize() - padding) / strideWidth);
        if (columns < 1 || (long) strideHeight + padding > options.spritePageSize()) throw new IllegalArgumentException(
            "One sprite cell exceeds the page size; increase it or choose a PNG sequence");
        int width = Math.addExact(Math.multiplyExact(columns, strideWidth), padding);
        long frameBytes = frame.rgbaBytes();
        long scanlineBytes = Math.addExact(width * 8L + 131073L, frame.width() * 16L);
        long bandBytes = Math.multiplyExact(frameBytes, columns + 1L);
        if (scanlineBytes > options.memoryBudgetBytes() || bandBytes > options.memoryBudgetBytes() - scanlineBytes)
            throw new IllegalArgumentException(
                "Sprite frame rows exceed the memory budget; use fewer columns or smaller frames");
        int rows = (options.spritePageSize() - padding) / strideHeight;
        return new CaptureSpritePageLayout(
            columns,
            rows,
            width,
            strideWidth,
            strideHeight,
            padding,
            layout.alignment()
                .offsetX(cell.width() - frame.width()),
            layout.alignment()
                .offsetY(cell.height() - frame.height()));
    }

    public int framesPerPage() {
        return Math.multiplyExact(columns, rows);
    }

    public int height(int frames) {
        if (frames < 1 || frames > framesPerPage())
            throw new IllegalArgumentException("Sprite page frame counts must fit the page capacity");
        long actualRows = ((long) frames + columns - 1) / columns;
        return Math.toIntExact(Math.addExact(Math.multiplyExact(actualRows, strideHeight), padding));
    }

    public int x(int frame) {
        return padding + frame % columns * strideWidth + offsetX;
    }

    public int y(int frame) {
        return padding + frame / columns * strideHeight + offsetY;
    }
}
