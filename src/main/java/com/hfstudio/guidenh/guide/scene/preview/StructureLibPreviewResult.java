package com.hfstudio.guidenh.guide.scene.preview;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;

public class StructureLibPreviewResult {

    private final StructureLibPreviewTask.Type type;
    private final StructureLibPreviewStatus status;
    private final String bindingKey;
    private final int requestVersion;
    @Nullable
    private final String selectionKey;
    @Nullable
    private final StructureLibImportResult importResult;
    @Nullable
    private final StructureLibRuntimeFacade.ControlAnalysis controlAnalysis;
    @Nullable
    private final String userMessage;
    @Nullable
    private final String technicalMessage;

    private StructureLibPreviewResult(StructureLibPreviewTask.Type type, StructureLibPreviewStatus status,
        String bindingKey, int requestVersion, @Nullable String selectionKey,
        @Nullable StructureLibImportResult importResult,
        @Nullable StructureLibRuntimeFacade.ControlAnalysis controlAnalysis, @Nullable String userMessage,
        @Nullable String technicalMessage) {
        this.type = type;
        this.status = status;
        this.bindingKey = bindingKey;
        this.requestVersion = requestVersion;
        this.selectionKey = selectionKey;
        this.importResult = importResult;
        this.controlAnalysis = controlAnalysis;
        this.userMessage = userMessage;
        this.technicalMessage = technicalMessage;
    }

    public static StructureLibPreviewResult analyzeSuccess(String bindingKey, int requestVersion,
        StructureLibRuntimeFacade.ControlAnalysis controlAnalysis) {
        return new StructureLibPreviewResult(
            StructureLibPreviewTask.Type.ANALYZE_LIMITS,
            StructureLibPreviewStatus.SUCCESS,
            bindingKey,
            requestVersion,
            null,
            null,
            controlAnalysis,
            null,
            null);
    }

    public static StructureLibPreviewResult buildSuccess(String bindingKey, int requestVersion, String selectionKey,
        StructureLibImportResult importResult, @Nullable StructureLibRuntimeFacade.ControlAnalysis controlAnalysis) {
        return new StructureLibPreviewResult(
            StructureLibPreviewTask.Type.BUILD_SELECTION,
            StructureLibPreviewStatus.SUCCESS,
            bindingKey,
            requestVersion,
            selectionKey,
            importResult,
            controlAnalysis,
            null,
            null);
    }

    public static StructureLibPreviewResult failed(StructureLibPreviewTask.Type type, String bindingKey,
        int requestVersion, @Nullable String selectionKey, String userMessage, @Nullable String technicalMessage) {
        return new StructureLibPreviewResult(
            type,
            StructureLibPreviewStatus.FAILED,
            bindingKey,
            requestVersion,
            selectionKey,
            null,
            null,
            userMessage,
            technicalMessage);
    }

    public StructureLibPreviewTask.Type getType() {
        return type;
    }

    public StructureLibPreviewStatus getStatus() {
        return status;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public int getRequestVersion() {
        return requestVersion;
    }

    @Nullable
    public String getSelectionKey() {
        return selectionKey;
    }

    @Nullable
    public StructureLibImportResult getImportResult() {
        return importResult;
    }

    @Nullable
    public StructureLibRuntimeFacade.ControlAnalysis getControlAnalysis() {
        return controlAnalysis;
    }

    @Nullable
    public String getUserMessage() {
        return userMessage;
    }

    @Nullable
    public String getTechnicalMessage() {
        return technicalMessage;
    }

    public boolean isSuccess() {
        return status == StructureLibPreviewStatus.SUCCESS;
    }
}
