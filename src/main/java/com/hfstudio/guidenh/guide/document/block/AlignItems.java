package com.hfstudio.guidenh.guide.document.block;

import java.util.Locale;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;

import lombok.Getter;

@Getter
public enum AlignItems implements SerializedEnum {

    CENTER,
    START,
    END,
    BASELINE;

    private final String serializedName;

    AlignItems() {
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }

}
