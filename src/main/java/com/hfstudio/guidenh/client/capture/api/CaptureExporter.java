package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

public interface CaptureExporter {

    long WORKSPACE_OVERHEAD_BYTES = 524288L;

    String format();

    default long workspaceBytes(CaptureExportRequest request, CaptureOutputSize output) {
        return Math.addExact(Math.multiplyExact(output.rgbaBytes(), 3), WORKSPACE_OVERHEAD_BYTES);
    }

    CaptureOutputArtifact export(CaptureExportRequest request, CaptureFrame frame, Path target) throws IOException;
}
