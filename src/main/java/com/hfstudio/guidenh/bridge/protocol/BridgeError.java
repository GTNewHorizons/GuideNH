package com.hfstudio.guidenh.bridge.protocol;

import lombok.Getter;

@Getter
public class BridgeError {

    private final String code;
    private final String message;
    private final boolean retryable;

    public BridgeError(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

}
