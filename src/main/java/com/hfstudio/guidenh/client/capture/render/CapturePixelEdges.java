package com.hfstudio.guidenh.client.capture.render;

public class CapturePixelEdges {

    private CapturePixelEdges() {}

    private static double snap(double coordinate) {
        double integer = Math.rint(coordinate);
        double tolerance = Math.max(0.0001, Math.ulp((float) coordinate) * 4);
        return Math.abs(coordinate - integer) <= tolerance ? integer : coordinate;
    }

    public static int floor(double coordinate) {
        return (int) Math.floor(snap(coordinate));
    }

    public static int ceil(double coordinate) {
        return (int) Math.ceil(snap(coordinate));
    }
}
