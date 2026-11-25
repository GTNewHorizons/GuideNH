package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

public interface CaptureAnimationExporter {

    String format();

    default boolean directory() {
        return false;
    }

    default long workspaceBytes(CaptureExportRequest request, CaptureRecordingOptions options,
        CaptureOutputSize output) {
        return Math.addExact(
            Math.multiplyExact(output.rgbaBytes(), 3),
            output.width() * 8L + CaptureExporter.WORKSPACE_OVERHEAD_BYTES);
    }

    CaptureAnimationWriter open(CaptureExportRequest request, CaptureRecordingOptions options, Path target)
        throws IOException;
}
