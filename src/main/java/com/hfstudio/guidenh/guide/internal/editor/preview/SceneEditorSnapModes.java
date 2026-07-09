package com.hfstudio.guidenh.guide.internal.editor.preview;

import lombok.Getter;

@Getter
public class SceneEditorSnapModes {

    public static final SceneEditorSnapModes DEFAULT = new SceneEditorSnapModes(true, false, false, false);

    private final boolean pointEnabled;
    private final boolean lineEnabled;
    private final boolean faceEnabled;
    private final boolean centerEnabled;

    public SceneEditorSnapModes(boolean pointEnabled, boolean lineEnabled, boolean faceEnabled, boolean centerEnabled) {
        this.pointEnabled = pointEnabled;
        this.lineEnabled = lineEnabled;
        this.faceEnabled = faceEnabled;
        this.centerEnabled = centerEnabled;
    }

    public static SceneEditorSnapModes defaultModes() {
        return DEFAULT;
    }

    public boolean hasEnabledMode() {
        return pointEnabled || lineEnabled || faceEnabled || centerEnabled;
    }
}
