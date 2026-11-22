package com.hfstudio.guidenh.client.capture.api;

public record CaptureSessionState(String id) {

    public static final CaptureSessionState PREPARING = new CaptureSessionState("preparing");
    public static final CaptureSessionState EDITING = new CaptureSessionState("editing");
    public static final CaptureSessionState SAVING = new CaptureSessionState("saving");
    public static final CaptureSessionState CLOSED = new CaptureSessionState("closed");
}
