package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface CaptureRegionWriter {

    public CaptureOutputArtifact export(CaptureExportRequest request, CaptureRows rows, Path target) throws IOException;
}
