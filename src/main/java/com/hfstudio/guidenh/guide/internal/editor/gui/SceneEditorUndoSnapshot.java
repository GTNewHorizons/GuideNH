package com.hfstudio.guidenh.guide.internal.editor.gui;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

import lombok.Getter;

public class SceneEditorUndoSnapshot {

    @Getter
    private final String rawText;
    @Getter
    private final String lastAppliedText;
    @Getter
    private final SceneEditorSceneModel sceneModel;
    @Getter
    private final SceneEditorTextSyncController.ValidationKind validationKind;
    @Nullable
    private final String validationMessage;
    @Getter
    private final SceneEditorUndoUiState uiState;

    public SceneEditorUndoSnapshot(String rawText, String lastAppliedText, SceneEditorSceneModel sceneModel,
        SceneEditorTextSyncController.ValidationKind validationKind, @Nullable String validationMessage) {
        this(rawText, lastAppliedText, sceneModel, validationKind, validationMessage, SceneEditorUndoUiState.empty());
    }

    public SceneEditorUndoSnapshot(String rawText, String lastAppliedText, SceneEditorSceneModel sceneModel,
        SceneEditorTextSyncController.ValidationKind validationKind, @Nullable String validationMessage,
        @Nullable SceneEditorUndoUiState uiState) {
        this.rawText = rawText != null ? rawText : "";
        this.lastAppliedText = lastAppliedText != null ? lastAppliedText : "";
        this.sceneModel = sceneModel;
        this.validationKind = validationKind;
        this.validationMessage = validationMessage;
        this.uiState = uiState != null ? uiState : SceneEditorUndoUiState.empty();
    }

    @Nullable
    public String getValidationMessage() {
        return validationMessage;
    }

}
