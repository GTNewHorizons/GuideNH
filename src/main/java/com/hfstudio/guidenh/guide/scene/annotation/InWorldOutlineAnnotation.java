package com.hfstudio.guidenh.guide.scene.annotation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.scene.outline.OutlineMode;
import com.hfstudio.guidenh.guide.scene.outline.OutlineRegion;
import com.hfstudio.guidenh.guide.scene.outline.OutlineTargetScope;

import lombok.Getter;

@Getter
public class InWorldOutlineAnnotation extends InWorldAnnotation {

    public static final float DEFAULT_THICKNESS = 1f;
    public static final float MIN_THICKNESS = 0.25f;
    public static final float MAX_THICKNESS = 8f;
    public static final float DEFAULT_PULSE_PERIOD = 2f;
    public static final float MIN_PULSE_INTENSITY = 0.3f;

    @Nullable
    private final OutlineRegion region;
    @Nullable
    private final String sceneEntityId;
    private final OutlineTargetScope scope;
    @Nullable
    private final ColorValue color;
    private final float thickness;
    private final OutlineMode mode;
    private final boolean pulse;
    private final float pulsePeriod;

    public InWorldOutlineAnnotation(@Nullable OutlineRegion region, @Nullable String sceneEntityId,
        OutlineTargetScope scope, @Nullable ColorValue color, float thickness, OutlineMode mode, boolean pulse,
        float pulsePeriod) {
        this.region = region;
        this.sceneEntityId = sceneEntityId;
        this.scope = scope;
        this.color = color;
        this.thickness = Math.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
        this.mode = mode;
        this.pulse = pulse;
        this.pulsePeriod = pulsePeriod > 0f ? pulsePeriod : DEFAULT_PULSE_PERIOD;
    }

    public InWorldOutlineAnnotation translate(int dx, int dy, int dz) {
        return new InWorldOutlineAnnotation(
            region != null ? region.translate(dx, dy, dz) : null,
            sceneEntityId,
            scope,
            color,
            thickness,
            mode,
            pulse,
            pulsePeriod);
    }

    public float pulseIntensity(long timeMillis) {
        if (!pulse) {
            return 1f;
        }
        double periodMillis = pulsePeriod * 1000.0;
        double phase = (timeMillis % (long) Math.max(1.0, periodMillis)) / periodMillis;
        float wave = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        return MIN_PULSE_INTENSITY + (1f - MIN_PULSE_INTENSITY) * wave;
    }

    public boolean outlinesRegionBlocks() {
        return region != null && scope.isBlocks();
    }

    public boolean targetsEntities() {
        return sceneEntityId != null || scope.isEntities();
    }

    @Nullable
    public OutlineRegion layerCullingRegion() {
        return sceneEntityId == null && scope == OutlineTargetScope.BLOCKS ? region : null;
    }
}
