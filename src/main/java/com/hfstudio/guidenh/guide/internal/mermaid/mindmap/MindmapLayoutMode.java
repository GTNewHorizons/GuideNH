package com.hfstudio.guidenh.guide.internal.mermaid.mindmap;

public enum MindmapLayoutMode {

    MINDMAP,
    TIDY_TREE;

    public static MindmapLayoutMode fromConfigValue(String value) {
        if (value == null) {
            return MINDMAP;
        }
        return switch (value.trim()
            .toLowerCase()) {
            case "tidy-tree" -> TIDY_TREE;
            default -> MINDMAP;
        };
    }
}
