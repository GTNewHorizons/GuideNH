package com.hfstudio.guidenh.guide.scene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;

public class StructureLibSceneBinding {

    @Nullable
    private final String name;
    private final String bindingKey;
    @Nullable
    private StructureLibSceneMetadata metadata;
    private int currentTier = StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
    private final LinkedHashMap<String, Integer> channelOverrides = new LinkedHashMap<>();
    @Nullable
    private Consumer<StructureLibPreviewSelection> selectionChangeListener;
    @Nullable
    private StructureLibPreviewSelection pendingSelection;
    private StructureLibSceneOptions rebuildOptions;
    private int rebuildOffsetX, rebuildOffsetY, rebuildOffsetZ;
    private boolean rebuildFormed;
    private Map<String, Boolean> rebuildIntegrationOptions = Map.of();
    private boolean hasRebuildRecipe;

    public StructureLibSceneBinding(@Nullable String name, String bindingKey) {
        this.name = StructureLibSceneCondition.normalizeStructureName(name);
        this.bindingKey = Objects.requireNonNull(bindingKey, "bindingKey");
    }

    @Nullable
    public String getName() {
        return name;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    @Nullable
    public StructureLibSceneMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(@Nullable StructureLibSceneMetadata metadata) {
        StructureLibPreviewSelection preservedSelection = pendingSelection != null ? pendingSelection
            : this.metadata != null ? getPreviewSelection() : null;
        this.metadata = metadata;
        channelOverrides.clear();
        if (metadata == null) {
            currentTier = StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
            return;
        }
        StructureLibSceneMetadata.TierData tierData = metadata.getTierData();
        currentTier = tierData != null ? tierData.getCurrentValue() : StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
        for (StructureLibSceneMetadata.ChannelData channelData : metadata.getChannelDataList()) {
            if (channelData != null && channelData.getCurrentValue() > 0) {
                channelOverrides.put(channelData.getChannelId(), channelData.getCurrentValue());
            }
        }
        if (preservedSelection != null) {
            applyPreviewSelection(preservedSelection);
        }
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(int currentTier) {
        StructureLibSceneMetadata.TierData tierData = metadata != null ? metadata.getTierData() : null;
        if (tierData == null) {
            this.currentTier = Math.max(1, currentTier);
            return;
        }
        this.currentTier = StructureLibSceneMetadata.clamp(currentTier, tierData.getMinValue(), tierData.getMaxValue());
    }

    public int getChannelValue(String channelId) {
        Integer value = channelOverrides.get(StructureLibPreviewSelection.normalizeChannelId(channelId));
        return value != null ? value : 0;
    }

    public void setChannelValue(String channelId, int value) {
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        if (normalized == null) {
            return;
        }
        StructureLibSceneMetadata.ChannelData channelData = metadata != null ? metadata.getChannelData(normalized)
            : null;
        int nextValue = channelData != null
            ? StructureLibSceneMetadata.clamp(value, channelData.getMinValue(), channelData.getMaxValue())
            : Math.max(0, value);
        if (nextValue > 0) {
            channelOverrides.put(normalized, nextValue);
        } else {
            channelOverrides.remove(normalized);
        }
    }

    public StructureLibPreviewSelection getPreviewSelection() {
        return new StructureLibPreviewSelection(currentTier, channelOverrides);
    }

    public void applyPreviewSelection(@Nullable StructureLibPreviewSelection previewSelection) {
        if (previewSelection == null) {
            return;
        }
        setCurrentTier(previewSelection.getMasterTier());
        channelOverrides.clear();
        for (Map.Entry<String, Integer> entry : previewSelection.getChannelOverrides()
            .entrySet()) {
            setChannelValue(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Integer> getChannelOverrides() {
        return channelOverrides;
    }

    @Nullable
    public Consumer<StructureLibPreviewSelection> getSelectionChangeListener() {
        return selectionChangeListener;
    }

    public void setSelectionChangeListener(@Nullable Consumer<StructureLibPreviewSelection> selectionChangeListener) {
        this.selectionChangeListener = selectionChangeListener;
    }

    @Nullable
    public StructureLibPreviewSelection getPendingSelection() {
        return pendingSelection;
    }

    public void setPendingSelection(@Nullable StructureLibPreviewSelection pendingSelection) {
        this.pendingSelection = pendingSelection;
    }

    public void setRebuildRecipe(StructureLibSceneOptions options, int offsetX, int offsetY, int offsetZ,
        boolean formed, Map<String, Boolean> integrationOptions) {
        this.rebuildOptions = options;
        this.rebuildOffsetX = offsetX;
        this.rebuildOffsetY = offsetY;
        this.rebuildOffsetZ = offsetZ;
        this.rebuildFormed = formed;
        this.rebuildIntegrationOptions = integrationOptions != null ? Map.copyOf(integrationOptions) : Map.of();
        this.hasRebuildRecipe = true;
    }

    public boolean hasRebuildRecipe() {
        return hasRebuildRecipe;
    }

    public StructureLibImportRequest buildRebuildRequest() {
        if (!hasRebuildRecipe || metadata == null) {
            return null;
        }
        StructureLibPreviewSelection selection = getPreviewSelection();
        for (Map.Entry<String, Boolean> entry : rebuildIntegrationOptions.entrySet()) {
            selection = selection.withIntegrationOption(entry.getKey(), entry.getValue());
        }
        return new StructureLibImportRequest(
            metadata.getController(),
            metadata.getPiece(),
            metadata.getFacing(),
            metadata.getRotation(),
            metadata.getFlip(),
            Integer.valueOf(selection.getMasterTier()),
            selection,
            rebuildOptions);
    }

    public int getRebuildOffsetX() {
        return rebuildOffsetX;
    }

    public int getRebuildOffsetY() {
        return rebuildOffsetY;
    }

    public int getRebuildOffsetZ() {
        return rebuildOffsetZ;
    }

    public boolean isRebuildFormed() {
        return rebuildFormed;
    }
}
