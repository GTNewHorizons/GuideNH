package com.hfstudio.guidenh.guide.scene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.structurelib.StructureLibBuildRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;

import lombok.Getter;

/**
 * Per-structure binding holding slider state (tier, channels) and rebuild recipe.
 * Single source of truth for the current preview selection.
 */
public class StructureLibSceneBinding {

    @Nullable
    private final String name;
    @Getter
    private final String bindingKey;
    @Nullable
    private StructureLibSceneMetadata metadata;
    @Getter
    private int currentTier = StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
    private final LinkedHashMap<String, Integer> channelOverrides = new LinkedHashMap<>();
    @Nullable
    private Consumer<StructureLibPreviewSelection> selectionChangeListener;

    // Rebuild recipe — set once during compile
    private StructureLibBuildRequest rebuildRequestTemplate;
    @Getter
    private int rebuildOffsetX;
    @Getter
    private int rebuildOffsetY;
    @Getter
    private int rebuildOffsetZ;
    @Getter
    private boolean rebuildFormed;
    private boolean hasRebuildRecipe;
    @Nullable
    private StructureLibImportResult lastSuccessfulImportResult;

    public StructureLibSceneBinding(@Nullable String name, String bindingKey) {
        this.name = StructureLibSceneCondition.normalizeStructureName(name);
        this.bindingKey = Objects.requireNonNull(bindingKey, "bindingKey");
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public StructureLibSceneMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(@Nullable StructureLibSceneMetadata metadata) {
        this.metadata = metadata;
        if (metadata == null) {
            currentTier = StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
            channelOverrides.clear();
            return;
        }
        channelOverrides.clear();
        StructureLibSceneMetadata.TierData td = metadata.getTierData();
        currentTier = td != null ? td.getCurrentValue() : StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
        for (StructureLibSceneMetadata.ChannelData cd : metadata.getChannelDataList()) {
            if (cd != null && cd.getCurrentValue() > 0) {
                channelOverrides.put(cd.getChannelId(), cd.getCurrentValue());
            }
        }
    }

    public void setCurrentTier(int tier) {
        StructureLibSceneMetadata.TierData td = metadata != null ? metadata.getTierData() : null;
        this.currentTier = td != null ? StructureLibSceneMetadata.clamp(tier, td.getMinValue(), td.getMaxValue())
            : Math.max(1, tier);
    }

    public int getChannelValue(String channelId) {
        Integer v = channelOverrides.get(StructureLibPreviewSelection.normalizeChannelId(channelId));
        return v != null ? v : 0;
    }

    public void setChannelValue(String channelId, int value) {
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        if (normalized == null) return;
        StructureLibSceneMetadata.ChannelData cd = metadata != null ? metadata.getChannelData(normalized) : null;
        int next = cd != null ? StructureLibSceneMetadata.clamp(value, cd.getMinValue(), cd.getMaxValue())
            : Math.max(0, value);
        if (next > 0) channelOverrides.put(normalized, next);
        else channelOverrides.remove(normalized);
    }

    public StructureLibPreviewSelection getPreviewSelection() {
        return new StructureLibPreviewSelection(currentTier, channelOverrides);
    }

    public Map<String, Integer> getChannelOverrides() {
        return channelOverrides;
    }

    @Nullable
    private StructureLibPreviewSelection pendingSelection;

    @Nullable
    public StructureLibPreviewSelection getPendingSelection() {
        return pendingSelection;
    }

    public void setPendingSelection(@Nullable StructureLibPreviewSelection pendingSelection) {
        this.pendingSelection = pendingSelection;
    }

    public void applyPreviewSelection(StructureLibPreviewSelection selection) {
        if (selection == null) return;
        setCurrentTier(selection.getMasterTier());
        channelOverrides.clear();
        for (Map.Entry<String, Integer> entry : selection.getChannelOverrides()
            .entrySet()) {
            setChannelValue(entry.getKey(), entry.getValue());
        }
    }

    public void setRebuildRecipe(StructureLibBuildRequest request, int offsetX, int offsetY, int offsetZ,
        boolean formed) {
        this.rebuildRequestTemplate = request;
        this.rebuildOffsetX = offsetX;
        this.rebuildOffsetY = offsetY;
        this.rebuildOffsetZ = offsetZ;
        this.rebuildFormed = formed;
        this.hasRebuildRecipe = true;
    }

    public boolean hasRebuildRecipe() {
        return hasRebuildRecipe;
    }

    @Nullable
    public StructureLibBuildRequest buildRebuildRequest() {
        if (!hasRebuildRecipe || metadata == null || rebuildRequestTemplate == null) return null;
        StructureLibBuildRequest req = new StructureLibBuildRequest(
            metadata.getController(),
            rebuildRequestTemplate.piece(),
            rebuildRequestTemplate.facing(),
            rebuildRequestTemplate.rotation(),
            rebuildRequestTemplate.flip(),
            currentTier,
            channelOverrides,
            rebuildRequestTemplate.options());
        return req;
    }

    @Nullable
    public Consumer<StructureLibPreviewSelection> getSelectionChangeListener() {
        return selectionChangeListener;
    }

    public void setSelectionChangeListener(@Nullable Consumer<StructureLibPreviewSelection> listener) {
        this.selectionChangeListener = listener;
    }

    @Nullable
    public StructureLibImportResult getLastSuccessfulImportResult() {
        return lastSuccessfulImportResult;
    }

    public void setLastSuccessfulImportResult(@Nullable StructureLibImportResult result) {
        this.lastSuccessfulImportResult = result != null && result.isSuccess() ? result : null;
    }
}
