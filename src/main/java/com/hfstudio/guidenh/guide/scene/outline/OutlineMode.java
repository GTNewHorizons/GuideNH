package com.hfstudio.guidenh.guide.scene.outline;

import java.util.Locale;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;

import lombok.Getter;

@Getter
public enum OutlineMode implements SerializedEnum {

    AUTO,
    GEOMETRY,
    CUTOUT,
    BOUNDS;

    private final String serializedName;

    OutlineMode() {
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }
}
