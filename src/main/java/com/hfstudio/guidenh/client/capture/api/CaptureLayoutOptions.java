package com.hfstudio.guidenh.client.capture.api;

public record CaptureLayoutOptions(String arrangement, int columns, int spacing) {

    public CaptureLayoutOptions {
        if (columns < 1 || spacing < 0) throw new IllegalArgumentException("Invalid capture arrangement");
    }
}
