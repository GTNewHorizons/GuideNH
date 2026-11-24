package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

public interface CaptureAnimationExporter {

    String format();

    default boolean directory() {
        return false;
    }

    CaptureAnimationWriter open(CaptureExportRequest request, CaptureRecordingOptions options, Path target)
        throws IOException;
}
