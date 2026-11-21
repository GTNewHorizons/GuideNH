package com.hfstudio.guidenh.guide.scene.outline;

import java.util.Locale;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;

import lombok.Getter;

@Getter
public enum OutlineTargetScope implements SerializedEnum {

    BLOCKS(true, false),
    ENTITIES(false, true),
    ALL(true, true);

    private final String serializedName;
    private final boolean blocks;
    private final boolean entities;

    OutlineTargetScope(boolean blocks, boolean entities) {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.blocks = blocks;
        this.entities = entities;
    }
}
