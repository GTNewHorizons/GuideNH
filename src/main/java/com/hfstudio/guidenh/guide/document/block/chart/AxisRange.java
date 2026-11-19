package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Utility for automatic axis range and tick calculation.
 */
public class AxisRange {

    public final double min;
    public final double max;
    public final double step;

    public AxisRange(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /**
     * Computes a "nice number" axis range from the data range; explicit min/max/step take precedence.
     */
    public static AxisRange compute(Double explicitMin, Double explicitMax, Double explicitStep, double dataMin,
        double dataMax) {
        double dMin = dataMin;
        double dMax = dataMax;
        if (!Double.isFinite(dMin) || !Double.isFinite(dMax) || dMin > dMax) {
            dMin = 0d;
            dMax = 1d;
        }
        if (dMax == dMin) {
            if (dMin == 0) {
                dMin = 0d;
                dMax = 1d;
            } else {
                double pad = Math.max(Math.abs(dMin) * 0.5, Math.ulp(dMin));
                if (Double.isFinite(dMin - pad)) dMin -= pad;
                if (Double.isFinite(dMax + pad)) dMax += pad;
            }
        }
        boolean hasMin = explicitMin != null && Double.isFinite(explicitMin);
        boolean hasMax = explicitMax != null && Double.isFinite(explicitMax);
        double low = hasMin ? explicitMin : dMin;
        double high = hasMax ? explicitMax : dMax;
        double range = niceNumber(high - low, false);
        double step;
        if (explicitStep != null && Double.isFinite(explicitStep) && explicitStep > 0) {
            step = explicitStep;
        } else {
            step = niceNumber(Double.isFinite(range) ? range / 5d : high / 5d - low / 5d, true);
        }
        double niceMin = hasMin ? low : Math.floor(low / step) * step;
        double niceMax = hasMax ? high : Math.ceil(high / step) * step;
        if (!Double.isFinite(niceMin)) niceMin = low;
        if (!Double.isFinite(niceMax)) niceMax = high;
        if (niceMax <= niceMin) {
            niceMax = niceMin + step;
            if (!Double.isFinite(niceMax) || niceMax <= niceMin) {
                niceMax = Math.nextUp(niceMin);
                if (!Double.isFinite(niceMax)) {
                    niceMax = niceMin;
                    niceMin = Math.nextDown(niceMin);
                }
            }
        }
        return new AxisRange(niceMin, niceMax, step);
    }

    private static double niceNumber(double value, boolean round) {
        if (value <= 0d) {
            return 1d;
        }
        double exp = Math.floor(Math.log10(value));
        double scale = Math.pow(10d, exp);
        if (scale == 0 || !Double.isFinite(scale)) return value;
        double frac = value / scale;
        double nf;
        if (round) {
            if (frac < 1.5) {
                nf = 1d;
            } else if (frac < 3d) {
                nf = 2d;
            } else if (frac < 7d) {
                nf = 5d;
            } else {
                nf = 10d;
            }
        } else {
            if (frac <= 1d) {
                nf = 1d;
            } else if (frac <= 2d) {
                nf = 2d;
            } else if (frac <= 5d) {
                nf = 5d;
            } else {
                nf = 10d;
            }
        }
        double result = nf * scale;
        return Double.isFinite(result) ? result : value;
    }

    public double normalize(double v) {
        return normalize(v, min, max);
    }

    public static double normalize(double value, double min, double max) {
        double span = max - min;
        if (!(max > min)) return 0;
        return Double.isFinite(span) ? (value - min) / span : (value / 2 - min / 2) / (max / 2 - min / 2);
    }
}
