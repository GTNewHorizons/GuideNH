package com.hfstudio.guidenh.client.capture.api;

public enum CaptureScheduleCondition {

    FOCUS("focus"),
    MINIMIZED("minimized"),
    NO_GUI("no_gui"),
    PAUSED("paused"),
    TARGET("target");

    private final String id;

    private CaptureScheduleCondition(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
