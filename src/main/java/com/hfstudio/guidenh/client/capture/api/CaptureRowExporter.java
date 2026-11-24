package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

public interface CaptureRowExporter extends CaptureExporter {

    public CaptureOutputArtifact exportRows(CaptureExportRequest request, CaptureRows rows, Path target)
        throws IOException;
}
