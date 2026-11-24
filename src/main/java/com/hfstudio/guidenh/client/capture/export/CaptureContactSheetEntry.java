package com.hfstudio.guidenh.client.capture.export;

import java.nio.file.Path;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public record CaptureContactSheetEntry(String targetId, String label, Path source, CaptureRect sourceBounds) {

    public CaptureContactSheetEntry(String targetId, String label, Path source) {
        this(targetId, label, source, null);
    }
}
