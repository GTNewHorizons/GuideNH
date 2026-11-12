package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.Locale;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;

/** Immutable color animation settings shared by annotations and site export. */
public record AnnotationColorStyle(Mode mode, float period) {

    public static final float DEFAULT_PERIOD = 2.4f;
    public static final AnnotationColorStyle SOLID = new AnnotationColorStyle(Mode.SOLID, DEFAULT_PERIOD);

    public AnnotationColorStyle {
        if (mode == null) {
            throw new IllegalArgumentException("Annotation colorMode must be solid or rainbow");
        }
        if (!Float.isFinite(period) || period <= 0f) {
            throw new IllegalArgumentException("Annotation colorPeriod must be a positive finite number of seconds");
        }
    }

    public static AnnotationColorStyle parse(String mode, float period) {
        Mode parsed = switch (mode) {
            case "solid" -> Mode.SOLID;
            case "rainbow" -> Mode.RAINBOW;
            default -> throw new IllegalArgumentException("Annotation colorMode must be solid or rainbow");
        };
        return new AnnotationColorStyle(parsed, period);
    }

    public boolean isRainbow() {
        return mode == Mode.RAINBOW;
    }

    public float phase(long timeMillis) {
        double periodMillis = period * 1000.0;
        return (float) ((timeMillis % periodMillis) / periodMillis);
    }

    public enum Mode implements SerializedEnum {

        SOLID,
        RAINBOW;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getSerializedName() {
            return serializedName();
        }
    }
}
