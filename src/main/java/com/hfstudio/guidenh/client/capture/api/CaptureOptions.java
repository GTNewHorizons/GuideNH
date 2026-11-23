package com.hfstudio.guidenh.client.capture.api;

import java.nio.file.Path;

public record CaptureOptions(CaptureResolution resolution, CaptureContentOptions content, CaptureLayoutOptions layout,
    Path outputDirectory, long temporaryBudgetBytes, CaptureBackendSelection backend) {

    public CaptureOptions(CaptureResolution resolution, CaptureContentOptions content, CaptureLayoutOptions layout,
        Path outputDirectory, long temporaryBudgetBytes) {
        this(resolution, content, layout, outputDirectory, temporaryBudgetBytes, CaptureBackendSelection.automatic());
    }

    public CaptureOptions withBackend(CaptureBackendSelection selection) {
        return new CaptureOptions(resolution, content, layout, outputDirectory, temporaryBudgetBytes, selection);
    }
}
