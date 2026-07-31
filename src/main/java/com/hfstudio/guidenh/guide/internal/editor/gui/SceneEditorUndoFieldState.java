package com.hfstudio.guidenh.guide.internal.editor.gui;

import lombok.Getter;

public class SceneEditorUndoFieldState {

    @Getter
    private final String draftText;
    private final boolean validationError;

    public SceneEditorUndoFieldState(String draftText, boolean validationError) {
        this.draftText = draftText != null ? draftText : "";
        this.validationError = validationError;
    }

    public boolean hasValidationError() {
        return validationError;
    }
}
