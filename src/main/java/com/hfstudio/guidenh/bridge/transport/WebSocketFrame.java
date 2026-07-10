package com.hfstudio.guidenh.bridge.transport;

import lombok.Getter;

@Getter
public class WebSocketFrame {

    private final int opcode;
    private final byte[] payload;

    public WebSocketFrame(int opcode, byte[] payload) {
        this.opcode = opcode;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public boolean isText() {
        return opcode == 1;
    }

    public boolean isClose() {
        return opcode == 8;
    }

    public boolean isPing() {
        return opcode == 9;
    }
}
