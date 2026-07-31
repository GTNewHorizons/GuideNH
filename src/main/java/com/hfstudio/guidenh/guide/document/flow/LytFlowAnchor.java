package com.hfstudio.guidenh.guide.document.flow;

import java.util.OptionalInt;

import lombok.Getter;
import lombok.Setter;

/**
 * Zero-Width Flow-Content that can be referred to by links.
 */
public class LytFlowAnchor extends LytFlowContent {

    @Getter
    private final String name;

    @Setter
    private int layoutY;

    public LytFlowAnchor(String name) {
        this.name = name;
    }

    public OptionalInt getLayoutY() {
        return layoutY >= 0 ? OptionalInt.of(layoutY) : OptionalInt.empty();
    }

}
