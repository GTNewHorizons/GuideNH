package com.hfstudio.guidenh.client.capture.api;

public record CaptureInputEvent(int key, boolean pressed, boolean control, boolean shift, String text) {}
