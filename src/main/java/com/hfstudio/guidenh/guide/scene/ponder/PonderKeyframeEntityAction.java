package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class PonderKeyframeEntityAction {

    @Nullable
    private String ref;
    @Nullable
    private String id;
    @Nullable
    private Double x;
    @Nullable
    private Double y;
    @Nullable
    private Double z;
    @Nullable
    private Float yaw;
    @Nullable
    private Float pitch;
    @Nullable
    private Float bodyYaw;
    @Nullable
    private Float headYaw;
    @Nullable
    private String nbt;
    @Nullable
    private String path;
    @Nullable
    private String value;
    @Nullable
    private String name;
    @Nullable
    private String nameKey;
    @Getter(AccessLevel.NONE)
    private transient String localizationFallbackName;
    @Getter(AccessLevel.NONE)
    private transient boolean localizationFallbackCaptured;
    @Nullable
    private String uuid;
    @Nullable
    private String sceneEntityId;
    @Nullable
    private String mount;
    @Nullable
    private Boolean unmount;
    @Nullable
    private Boolean remove;
    @Nullable
    private Boolean showName;
    @Nullable
    private Boolean showCape;
    @Nullable
    private Boolean baby;
    @Nullable
    private String headRotation;
    @Nullable
    private String leftArmRotation;
    @Nullable
    private String rightArmRotation;
    @Nullable
    private String leftLegRotation;
    @Nullable
    private String rightLegRotation;
    @Nullable
    private String capeRotation;

    private void captureLocalizationFallback() {
        if (!localizationFallbackCaptured) {
            localizationFallbackName = name;
            localizationFallbackCaptured = true;
        }
    }

    public void applyLocalizedName(@Nullable String localizedName) {
        captureLocalizationFallback();
        this.name = localizedName != null && !localizedName.isEmpty() ? localizedName : localizationFallbackName;
    }

}
