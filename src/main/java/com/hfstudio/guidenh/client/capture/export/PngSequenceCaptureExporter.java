package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.file.Path;

import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;

public class PngSequenceCaptureExporter implements CaptureAnimationExporter {

    private final String format;

    public PngSequenceCaptureExporter(String format) {
        this.format = format;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public boolean directory() {
        return true;
    }

    @Override
    public CaptureAnimationWriter open(CaptureExportRequest request, CaptureRecordingOptions options, Path target)
        throws IOException {
        return new PngSequenceCaptureWriter(request, options, target);
    }
}
