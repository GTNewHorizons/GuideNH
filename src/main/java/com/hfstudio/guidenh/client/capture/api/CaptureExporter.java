package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

public interface CaptureExporter {

    String format();

    CaptureOutputArtifact export(CaptureExportRequest request, CaptureFrame frame, Path target) throws IOException;
}
