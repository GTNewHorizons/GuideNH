package com.hfstudio.guidenh.client.capture.api;

import java.util.Objects;

public class CaptureImmediateReadback implements CaptureReadback {

    private CaptureFrame frame;

    public CaptureImmediateReadback(CaptureFrame frame) {
        this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public boolean ready() {
        if (frame == null) throw new IllegalStateException("The readback has been consumed or closed");
        return true;
    }

    @Override
    public CaptureFrame take() {
        ready();
        CaptureFrame result = frame;
        frame = null;
        return result;
    }

    @Override
    public void close() {
        if (frame != null) frame.close();
        frame = null;
    }
}
