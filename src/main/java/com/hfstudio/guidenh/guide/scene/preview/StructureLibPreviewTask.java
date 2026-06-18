package com.hfstudio.guidenh.guide.scene.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;

public class StructureLibPreviewTask {

    public enum Type {
        ANALYZE_LIMITS,
        BUILD_SELECTION
    }

    public enum Priority {
        NORMAL,
        HIGH
    }

    private final Type type;
    private final String bindingKey;
    private final int requestVersion;
    @Nullable
    private final String selectionKey;
    private final Priority priority;
    private final StructureLibImportRequest request;
    @Nullable
    private final StructureLibRuntimeFacade.ControlAnalysis controlAnalysis;

    private StructureLibPreviewTask(Type type, String bindingKey, int requestVersion, @Nullable String selectionKey,
        Priority priority, StructureLibImportRequest request,
        @Nullable StructureLibRuntimeFacade.ControlAnalysis controlAnalysis) {
        this.type = type;
        this.bindingKey = bindingKey;
        this.requestVersion = requestVersion;
        this.selectionKey = selectionKey;
        this.priority = priority;
        this.request = request;
        this.controlAnalysis = controlAnalysis;
    }

    public static StructureLibPreviewTask analyzeLimits(String bindingKey, int requestVersion,
        StructureLibImportRequest request) {
        return new StructureLibPreviewTask(
            Type.ANALYZE_LIMITS,
            bindingKey,
            requestVersion,
            null,
            Priority.NORMAL,
            request,
            null);
    }

    public static StructureLibPreviewTask buildSelection(String bindingKey, int requestVersion,
        StructureLibImportRequest request, Priority priority,
        @Nullable StructureLibRuntimeFacade.ControlAnalysis controlAnalysis) {
        return new StructureLibPreviewTask(
            Type.BUILD_SELECTION,
            bindingKey,
            requestVersion,
            selectionKeyOf(request.getPreviewSelection()),
            priority,
            request,
            controlAnalysis);
    }

    public Type getType() {
        return type;
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

    public Priority getPriority() {
        return priority;
    }

    public StructureLibImportRequest getRequest() {
        return request;
    }

    @Nullable
    public StructureLibRuntimeFacade.ControlAnalysis getControlAnalysis() {
        return controlAnalysis;
    }

    public boolean isSelectionBuild() {
        return type == Type.BUILD_SELECTION;
    }

    public static String selectionKeyOf(@Nullable StructureLibPreviewSelection selection) {
        StructureLibPreviewSelection effectiveSelection = selection != null ? selection
            : StructureLibPreviewSelection.defaultSelection();
        StringBuilder builder = new StringBuilder();
        builder.append("tier=")
            .append(effectiveSelection.getMasterTier());

        List<String> channelIds = new ArrayList<>(
            effectiveSelection.getChannelOverrides()
                .keySet());
        Collections.sort(channelIds);
        for (String channelId : channelIds) {
            builder.append(";channel:")
                .append(channelId)
                .append('=')
                .append(effectiveSelection.getChannelValue(channelId));
        }

        List<String> optionIds = new ArrayList<>(
            effectiveSelection.getIntegrationOptions()
                .keySet());
        Collections.sort(optionIds);
        for (String optionId : optionIds) {
            builder.append(";option:")
                .append(optionId)
                .append('=')
                .append(
                    Boolean.TRUE.equals(
                        effectiveSelection.getIntegrationOptions()
                            .get(optionId)));
        }
        return builder.toString();
    }
}
