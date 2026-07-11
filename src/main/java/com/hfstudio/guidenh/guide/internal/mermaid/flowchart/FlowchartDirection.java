package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

public enum FlowchartDirection {

    TB,
    BT,
    LR,
    RL;

    public static FlowchartDirection fromString(String value) {
        if (value == null) return TB;
        return switch (value.trim()
            .toUpperCase(java.util.Locale.ROOT)) {
            case "BT" -> BT;
            case "LR" -> LR;
            case "RL" -> RL;
            default -> TB;
        };
    }
}
