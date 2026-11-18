package com.hfstudio.guidenh.guide.document.block.chart;

public record ChartTicks(double first, double step, int count) {

    private static final int MAX_COUNT = 256;

    public static ChartTicks create(double min, double max, double step) {
        double span = max - min;
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min || !Double.isFinite(step) || step <= 0) {
            return new ChartTicks(0, 1, 0);
        }
        double minimumStep = Math.max(
            Double.isFinite(span) ? span / (MAX_COUNT - 1) : max / (MAX_COUNT - 1) - min / (MAX_COUNT - 1),
            Math.ulp(Math.max(Math.abs(min), Math.abs(max))));
        if (step < minimumStep) {
            double scaledStep = Math.ceil(minimumStep / step) * step;
            step = Double.isFinite(scaledStep) ? scaledStep : minimumStep;
        }
        double intervals = Double.isFinite(span) ? span / step : max / step - min / step;
        int count = Math.min(MAX_COUNT, (int) Math.floor(intervals + 1e-9) + 1);
        if (!Double.isFinite(Math.fma(step, count - 1, min))) count--;
        return new ChartTicks(min, step, count);
    }

    public double value(int index) {
        return Math.fma(step, index, first);
    }
}
