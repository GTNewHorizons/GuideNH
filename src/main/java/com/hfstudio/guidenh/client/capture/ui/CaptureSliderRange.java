package com.hfstudio.guidenh.client.capture.ui;

import java.math.BigDecimal;

public record CaptureSliderRange(double minimum, double maximum, double step) {

    public CaptureSliderRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)
            || !Double.isFinite(step)
            || maximum <= minimum
            || step <= 0) throw new IllegalArgumentException("Invalid capture slider range");
    }

    public float position(String text) {
        return (float) Math.clamp((Double.parseDouble(text) - minimum) / (maximum - minimum), 0, 1);
    }

    public String value(float position) {
        double value = minimum + Math.clamp(position, 0, 1) * (maximum - minimum);
        long steps = Math.round((value - minimum) / step);
        value = Math.clamp(minimum + steps * step, minimum, maximum);
        return step >= 1 ? Long.toString(Math.round(value))
            : BigDecimal.valueOf(minimum)
                .add(
                    BigDecimal.valueOf(step)
                        .multiply(BigDecimal.valueOf(steps)))
                .max(BigDecimal.valueOf(minimum))
                .min(BigDecimal.valueOf(maximum))
                .stripTrailingZeros()
                .toPlainString();
    }
}
