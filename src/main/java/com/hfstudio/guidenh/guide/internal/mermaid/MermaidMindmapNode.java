package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class MermaidMindmapNode {

    @Getter
    private final String id;
    @Getter
    private final String labelSource;
    @Getter
    private final String text;
    @Getter
    private final MermaidMindmapNodeShape shape;
    @Getter
    private final List<String> classes;
    @Nullable
    private final String icon;
    @Nullable
    private final Integer x;
    @Nullable
    private final Integer y;
    private final List<MermaidMindmapNode> children = new ArrayList<>();

    public MermaidMindmapNode(String id, String labelSource, String text, MermaidMindmapNodeShape shape,
        List<String> classes, @Nullable String icon, @Nullable Integer x, @Nullable Integer y) {
        this.id = id != null ? id : "";
        this.labelSource = labelSource != null ? labelSource : "";
        this.text = text != null ? text : "";
        this.shape = shape != null ? shape : MermaidMindmapNodeShape.DEFAULT;
        this.classes = List.copyOf(new ArrayList<>(classes != null ? classes : List.of()));
        this.icon = icon != null && !icon.trim()
            .isEmpty() ? icon.trim() : null;
        this.x = x;
        this.y = y;
    }

    public @Nullable String getIcon() {
        return icon;
    }

    public @Nullable Integer getX() {
        return x;
    }

    public @Nullable Integer getY() {
        return y;
    }

    public List<MermaidMindmapNode> getChildren() {
        return List.copyOf(children);
    }

    public void addChild(MermaidMindmapNode child) {
        if (child != null) {
            children.add(child);
        }
    }
}
