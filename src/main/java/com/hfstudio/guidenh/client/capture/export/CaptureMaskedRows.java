package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class CaptureMaskedRows implements CaptureRows {

    private final CaptureRows source;
    private final CaptureEraseMask mask;
    private final int background;

    public CaptureMaskedRows(CaptureExportRequest request, CaptureRows source) {
        this.source = source;
        mask = new CaptureEraseMask(request, CaptureRowComposer.compose(request), source.size());
        background = request.options()
            .content()
            .backgroundArgb();
    }

    @Override
    public CaptureOutputSize size() {
        return source.size();
    }

    @Override
    public void row(int y, int[] target) throws IOException {
        source.row(y, target);
        mask.apply(y, target, background);
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
