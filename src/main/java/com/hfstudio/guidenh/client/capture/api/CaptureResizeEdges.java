package com.hfstudio.guidenh.client.capture.api;

public record CaptureResizeEdges(boolean left, boolean right, boolean top, boolean bottom) {

    public static final CaptureResizeEdges NONE = new CaptureResizeEdges(false, false, false, false);

    public boolean isEmpty() {
        return !left && !right && !top && !bottom;
    }
}
