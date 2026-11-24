package com.hfstudio.guidenh.client.capture.render;

public class CaptureRedrawMismatchException extends IllegalArgumentException {

    private final int x;
    private final int y;

    public CaptureRedrawMismatchException(int x, int y) {
        super("The draw snapshot cannot reproduce the captured source at " + x + "," + y);
        this.x = x;
        this.y = y;
    }

    public CaptureRedrawMismatchException(int x, int y, int original, int replayed) {
        super(
            "The draw snapshot cannot reproduce the captured source at " + x
                + ","
                + y
                + ": native="
                + Integer.toHexString(original)
                + ", replay="
                + Integer.toHexString(replayed));
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }
}
