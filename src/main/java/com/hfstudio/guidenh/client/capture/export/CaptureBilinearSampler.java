package com.hfstudio.guidenh.client.capture.export;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;

public class CaptureBilinearSampler {

    private CaptureBilinearSampler() {}

    public static int sample(CaptureFrame frame, CapturePlacement placement, double x, double y) {
        double localX = Math.clamp(x, 0, placement.width() - 1d);
        double localY = Math.clamp(y, 0, placement.height() - 1d);
        int left = (int) localX;
        int top = (int) localY;
        int right = Math.min(left + 1, placement.width() - 1);
        int bottom = Math.min(top + 1, placement.height() - 1);
        int first = frame.argb(placement.sourceX() + left, placement.sourceY() + top);
        int second = frame.argb(placement.sourceX() + right, placement.sourceY() + top);
        int third = frame.argb(placement.sourceX() + left, placement.sourceY() + bottom);
        int fourth = frame.argb(placement.sourceX() + right, placement.sourceY() + bottom);
        double horizontal = localX - left;
        double vertical = localY - top;
        // Weight colors by alpha so transparent texels cannot tint visible edges.
        double firstWeight = (1 - horizontal) * (1 - vertical) * (first >>> 24);
        double secondWeight = horizontal * (1 - vertical) * (second >>> 24);
        double thirdWeight = (1 - horizontal) * vertical * (third >>> 24);
        double fourthWeight = horizontal * vertical * (fourth >>> 24);
        double coverage = firstWeight + secondWeight + thirdWeight + fourthWeight;
        int alpha = (int) Math.round(coverage);
        if (alpha == 0) return 0;
        int red = channel(
            first,
            second,
            third,
            fourth,
            firstWeight,
            secondWeight,
            thirdWeight,
            fourthWeight,
            coverage,
            16);
        int green = channel(
            first,
            second,
            third,
            fourth,
            firstWeight,
            secondWeight,
            thirdWeight,
            fourthWeight,
            coverage,
            8);
        int blue = channel(
            first,
            second,
            third,
            fourth,
            firstWeight,
            secondWeight,
            thirdWeight,
            fourthWeight,
            coverage,
            0);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int channel(int first, int second, int third, int fourth, double firstWeight, double secondWeight,
        double thirdWeight, double fourthWeight, double coverage, int shift) {
        return (int) Math.round(
            (((first >>> shift) & 255) * firstWeight + ((second >>> shift) & 255) * secondWeight
                + ((third >>> shift) & 255) * thirdWeight
                + ((fourth >>> shift) & 255) * fourthWeight) / coverage);
    }
}
