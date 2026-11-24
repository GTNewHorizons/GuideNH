package com.hfstudio.guidenh.client.capture.api;

public record CaptureLayoutOptions(String arrangement, int columns, int spacing, boolean separateRegions) {

    public CaptureLayoutOptions(String arrangement, int columns, int spacing) {
        this(arrangement, columns, spacing, false);
    }

    public CaptureLayoutOptions {
        if (columns < 1 || spacing < 0) throw new IllegalArgumentException("Invalid capture arrangement");
    }
}
