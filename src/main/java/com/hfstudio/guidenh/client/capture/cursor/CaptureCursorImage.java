package com.hfstudio.guidenh.client.capture.cursor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureCursorImage {

    private final int width;
    private final int height;
    private final int hotspotX;
    private final int hotspotY;
    private final int[] pixels;
    private final List<CaptureRect> inverted;

    public CaptureCursorImage(int width, int height, int hotspotX, int hotspotY, int[] pixels, BitSet inverse) {
        if (width <= 0 || height <= 0 || pixels.length != Math.multiplyExact(width, height))
            throw new IllegalArgumentException("Invalid system cursor image");
        this.width = width;
        this.height = height;
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
        this.pixels = pixels.clone();
        List<CaptureRect> rectangles = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            int end = (y + 1) * width;
            for (int start = inverse.nextSetBit(y * width); start >= 0 && start < end;) {
                int right = Math.min(end, inverse.nextClearBit(start));
                rectangles.add(new CaptureRect(start % width, y, right - start, 1));
                start = inverse.nextSetBit(right);
            }
        }
        inverted = List.copyOf(rectangles);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int hotspotX() {
        return hotspotX;
    }

    public int hotspotY() {
        return hotspotY;
    }

    public List<CaptureRect> inverted() {
        return inverted;
    }

    public void copyPixels(int[] target) {
        System.arraycopy(pixels, 0, target, 0, pixels.length);
    }
}
