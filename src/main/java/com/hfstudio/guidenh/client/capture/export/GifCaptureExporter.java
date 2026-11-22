package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.file.Path;

import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;

public class GifCaptureExporter implements CaptureAnimationExporter {

    @Override
    public String format() {
        return "gif";
    }

    @Override
    public CaptureAnimationWriter open(CaptureExportRequest request, CaptureRecordingOptions options, Path target)
        throws IOException {
        return new GifCaptureWriter(request, options, target);
    }
}
