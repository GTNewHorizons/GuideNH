package com.hfstudio.guidenh.guide.internal.mermaid.mindmap;

public class MindmapDocument {

    private final MindmapLayoutMode layoutMode;
    private final MindmapNode root;

    public MindmapDocument(MindmapLayoutMode layoutMode, MindmapNode root) {
        this.layoutMode = layoutMode != null ? layoutMode : MindmapLayoutMode.MINDMAP;
        this.root = root;
    }

    public MindmapLayoutMode getLayoutMode() {
        return layoutMode;
    }

    public MindmapNode getRoot() {
        return root;
    }
}
