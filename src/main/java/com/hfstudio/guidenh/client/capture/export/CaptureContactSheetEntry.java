package com.hfstudio.guidenh.client.capture.export;

import java.nio.file.Path;

public record CaptureContactSheetEntry(String targetId, String label, Path source) {}
