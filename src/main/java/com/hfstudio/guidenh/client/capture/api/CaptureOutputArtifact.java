package com.hfstudio.guidenh.client.capture.api;

import java.nio.file.Path;

public record CaptureOutputArtifact(Path path, String format, int width, int height, int frameCount, long byteSize) {}
