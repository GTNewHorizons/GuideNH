package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class CaptureRegionRows implements CaptureRows {

    private final CaptureRows source;
    private final CaptureOutputSize size;
    private final int x;
    private final int y;
    private final int[] sourceRow;

    public CaptureRegionRows(CaptureRows source, int x, int y, int width, int height) {
        this.source = source;
        this.x = x;
        this.y = y;
        size = new CaptureOutputSize(width, height);
        sourceRow = new int[source.size()
            .width()];
    }

    @Override
    public CaptureOutputSize size() {
        return size;
    }

    @Override
    public void row(int row, int[] target) throws IOException {
        source.row(y + row, sourceRow);
        System.arraycopy(sourceRow, x, target, 0, size.width());
    }
}
