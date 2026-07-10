package com.hfstudio.guidenh.guide.internal.editor.md;

import java.util.UUID;

import lombok.Getter;

@Getter
public class SceneEditorMarkdownElementRange {

    private final UUID elementId;
    private final int startIndex;
    private final int endIndex;

    public SceneEditorMarkdownElementRange(UUID elementId, int startIndex, int endIndex) {
        this.elementId = elementId;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public boolean contains(int cursorIndex) {
        return cursorIndex >= startIndex && cursorIndex < endIndex;
    }
}
