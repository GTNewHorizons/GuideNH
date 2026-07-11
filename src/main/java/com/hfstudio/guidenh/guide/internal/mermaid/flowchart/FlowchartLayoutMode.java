package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.Locale;

public enum FlowchartLayoutMode {

    BUILTIN,
    ELK;

    public static FlowchartLayoutMode fromConfigValue(String value) {
        if (value == null) return BUILTIN;
        return switch (value.trim()
            .toLowerCase(Locale.ROOT)) {
            case "elk" -> ELK;
            default -> BUILTIN;
        };
    }
}
