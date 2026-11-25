package com.hfstudio.guidenh.client.capture.api;

public class CaptureMemoryLimitException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public CaptureMemoryLimitException(String message) {
        super(message);
    }
}
