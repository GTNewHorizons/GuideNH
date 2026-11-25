package com.hfstudio.guidenh.client.capture.api;

public record CaptureSpriteLayout(int cellWidth, int cellHeight, CaptureAlignment alignment) {

    public static final CaptureSpriteLayout NATIVE = new CaptureSpriteLayout(0, 0, CaptureAlignment.TOP_LEFT);

    public CaptureSpriteLayout {
        if (cellWidth < 0 || cellHeight < 0 || alignment == null)
            throw new IllegalArgumentException("Sprite cells require nonnegative dimensions and an alignment");
    }

    public CaptureOutputSize resolve(CaptureOutputSize frame) {
        int width = cellWidth == 0 ? frame.width() : cellWidth;
        int height = cellHeight == 0 ? frame.height() : cellHeight;
        if (width < frame.width() || height < frame.height())
            throw new IllegalArgumentException("Sprite cells must be large enough for the captured frame");
        return new CaptureOutputSize(width, height);
    }
}
