package com.hfstudio.guidenh.client.capture.api;

import java.nio.file.Path;

public record CaptureOptions(CaptureResolution resolution, CaptureContentOptions content, CaptureLayoutOptions layout,
    Path outputDirectory, long temporaryBudgetBytes) {}
