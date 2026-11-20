package com.hfstudio.guidenh.guide.document.block.chart;

public class PieChartGeometry {

    private static final double FULL_TURN = Math.PI * 2;

    protected PieChartGeometry() {}

    public static double positiveValue(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    public static double startAngle(float degrees) {
        return Math.toRadians(Float.isFinite(degrees) ? degrees % 360 : -90);
    }

    public static boolean containsAngle(double angle, double from, double to) {
        if (!Double.isFinite(angle) || !Double.isFinite(from) || !Double.isFinite(to)) return false;
        double sweep = to - from;
        if (sweep == 0) return false;
        if (Math.abs(sweep) >= FULL_TURN) return true;
        double relative = (angle % FULL_TURN - from % FULL_TURN) % FULL_TURN;
        if (sweep < 0) relative = -relative;
        if (relative < 0) relative += FULL_TURN;
        return relative < Math.abs(sweep);
    }
}
