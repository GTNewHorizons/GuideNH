package com.hfstudio.guidenh.client.capture.api;

public record CaptureLayoutOptions(String arrangement, int columns, int spacing, boolean separateRegions,
    CaptureAlignment alignment) {

    public CaptureLayoutOptions(String arrangement, int columns, int spacing) {
        this(arrangement, columns, spacing, false, CaptureAlignment.TOP_LEFT);
    }

    public CaptureLayoutOptions(String arrangement, int columns, int spacing, boolean separateRegions) {
        this(arrangement, columns, spacing, separateRegions, CaptureAlignment.TOP_LEFT);
    }

    public CaptureLayoutOptions {
        if (columns < 1 || spacing < 0) throw new IllegalArgumentException("Invalid capture arrangement");
        if (alignment == null) throw new IllegalArgumentException("A capture alignment is required");
    }
}
