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
        long pageBudget = options.memoryBudgetBytes() - frame.rgbaBytes();
        long maximumHeight = Math.max(0, pageBudget) / (width * 4L);
        long budgetRows = Math.max(0, (maximumHeight - padding) / strideHeight);
        int rows = (int) Math.min((options.spritePageSize() - padding) / strideHeight, budgetRows);
        if (rows < 1) throw new IllegalArgumentException(
            "Sprite pages exceed the memory budget; use a PNG sequence or smaller cells");
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
        return Math.addExact(Math.multiplyExact((frames + columns - 1) / columns, strideHeight), padding);
    }

    public int x(int frame) {
        return padding + frame % columns * strideWidth + offsetX;
    }

    public int y(int frame) {
        return padding + frame / columns * strideHeight + offsetY;
    }
}
