package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

/**
 * Metadata about a StructureLib preview — controller identity, tier/channel ranges, per-block tooltip data.
 * Tooltip data (hatch info, block candidates) is no longer produced; fields remain for backward compat.
 */
public class StructureLibSceneMetadata {

    @Getter
    private final String controller;
    @Nullable
    private final String piece;
    @Nullable
    private final String facing;
    @Nullable
    private final String rotation;
    @Nullable
    private final String flip;
    @Nullable
    private final TierData tierData;
    @Getter
    private final List<ChannelData> channelDataList;
    private final Map<String, ChannelData> channelDataById;
    private final Map<Long, BlockTooltipData> blockTooltipDataByPos;

    public StructureLibSceneMetadata(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip) {
        this(controller, piece, facing, rotation, flip, null, List.of(), Map.of());
    }

    private StructureLibSceneMetadata(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable TierData tierData,
        List<ChannelData> channelDataList, Map<Long, BlockTooltipData> blockTooltipDataByPos) {
        this.controller = requireController(controller);
        this.piece = normalizeOptional(piece);
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.tierData = tierData;
        this.channelDataList = immutableChannels(channelDataList);
        this.channelDataById = indexChannels(this.channelDataList);
        this.blockTooltipDataByPos = blockTooltipDataByPos != null ? blockTooltipDataByPos : Map.of();
    }

    // ========== Fluent factories (for programmatic construction) ==========

    public StructureLibSceneMetadata withTierData(int minValue, int maxValue, int defaultValue, int currentValue) {
        return new StructureLibSceneMetadata(
            controller,
            piece,
            facing,
            rotation,
            flip,
            new TierData(minValue, maxValue, defaultValue, currentValue),
            channelDataList,
            blockTooltipDataByPos);
    }

    public StructureLibSceneMetadata withChannelData(String channelId, String label, int maxValue, int currentValue) {
        LinkedHashMap<String, ChannelData> updated = new LinkedHashMap<>(channelDataById);
        ChannelData next = new ChannelData(channelId, label, maxValue, 0, currentValue);
        updated.put(next.getChannelId(), next);
        return new StructureLibSceneMetadata(
            controller,
            piece,
            facing,
            rotation,
            flip,
            tierData,
            new ArrayList<>(updated.values()),
            blockTooltipDataByPos);
    }

    // ========== Tooltip data (deprecated — always empty) ==========

    @Nullable
    public BlockTooltipData getBlockTooltipData(int x, int y, int z) {
        return null;
    }

    public List<BlockTooltipEntry> getHatchTooltipEntries() {
        return List.of();
    }

    public Set<Long> getHatchTooltipPositions() {
        return Set.of();
    }

    public boolean hasHatchTooltipData() {
        return false;
    }

    // ========== Getters ==========

    @Nullable
    public TierData getTierData() {
        return tierData;
    }

    @Nullable
    public ChannelData getChannelData(String channelId) {
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        return normalized != null ? channelDataById.get(normalized) : null;
    }

    public boolean hasSelectableChannels() {
        for (ChannelData cd : channelDataList) {
            if (cd.isSelectable()) return true;
        }
        return false;
    }

    @Nullable
    public String getPiece() {
        return piece;
    }

    @Nullable
    public String getFacing() {
        return facing;
    }

    @Nullable
    public String getRotation() {
        return rotation;
    }

    @Nullable
    public String getFlip() {
        return flip;
    }

    // ========== Position encoding ==========

    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public static int unpackBlockPosX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    public static int unpackBlockPosY(long packedPos) {
        return (int) (packedPos << 52 >> 52);
    }

    public static int unpackBlockPosZ(long packedPos) {
        return (int) (packedPos << 26 >> 38);
    }

    // ========== Statics ==========

    public static String requireController(@Nullable String controller) {
        if (controller == null) throw new IllegalArgumentException("StructureLib metadata controller cannot be null");
        String trimmed = controller.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("StructureLib metadata controller cannot be empty");
        return trimmed;
    }

    @Nullable
    public static String normalizeOptional(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static List<ChannelData> immutableChannels(@Nullable List<ChannelData> source) {
        if (source == null || source.isEmpty()) return List.of();
        LinkedHashMap<String, ChannelData> dedup = new LinkedHashMap<>(source.size());
        for (ChannelData cd : source) {
            if (cd != null) dedup.put(cd.getChannelId(), cd);
        }
        return dedup.isEmpty() ? List.of() : List.copyOf(dedup.values());
    }

    public static Map<String, ChannelData> indexChannels(List<ChannelData> channels) {
        if (channels.isEmpty()) return Map.of();
        LinkedHashMap<String, ChannelData> indexed = new LinkedHashMap<>(channels.size());
        for (ChannelData cd : channels) indexed.put(cd.getChannelId(), cd);
        return Map.copyOf(indexed);
    }

    public static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) return minValue;
        return Math.min(value, maxValue);
    }

    // ========== Inner types ==========

    @Getter
    public static class BlockTooltipEntry {

        private final int x;
        private final int y;
        private final int z;
        private final BlockTooltipData tooltipData;

        public BlockTooltipEntry(int x, int y, int z, BlockTooltipData tooltipData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.tooltipData = tooltipData;
        }
    }

    public static class BlockTooltipData {

        @Nullable
        private final String structureLibDescription;
        @Getter
        private final List<ItemStack> blockCandidates;
        @Getter
        private final List<StructureLibHatchDescriptionLine> hatchDescriptionLines;
        @Getter
        private final List<ItemStack> hatchCandidates;

        public BlockTooltipData(@Nullable String structureLibDescription, List<ItemStack> blockCandidates,
            List<StructureLibHatchDescriptionLine> hatchDescriptionLines, List<ItemStack> hatchCandidates) {
            this.structureLibDescription = normalizeOptional(structureLibDescription);
            this.blockCandidates = immutableStacks(blockCandidates);
            this.hatchDescriptionLines = immutableLines(hatchDescriptionLines);
            this.hatchCandidates = immutableStacks(hatchCandidates);
        }

        @Nullable
        public String getStructureLibDescription() {
            return structureLibDescription;
        }

        public boolean hasAdditionalTooltipContent() {
            return structureLibDescription != null || !blockCandidates.isEmpty()
                || !hatchDescriptionLines.isEmpty()
                || !hatchCandidates.isEmpty();
        }

        public boolean hasHatchDetails() {
            return !hatchDescriptionLines.isEmpty() || !hatchCandidates.isEmpty();
        }

        static List<ItemStack> immutableStacks(@Nullable List<ItemStack> stacks) {
            if (stacks == null || stacks.isEmpty()) return List.of();
            List<ItemStack> copied = new ArrayList<>(stacks.size());
            for (ItemStack s : stacks) {
                if (s != null && s.stackSize > 0) copied.add(s.copy());
            }
            return copied.isEmpty() ? List.of() : List.copyOf(copied);
        }

        static List<StructureLibHatchDescriptionLine> immutableLines(
            @Nullable List<StructureLibHatchDescriptionLine> lines) {
            if (lines == null || lines.isEmpty()) return List.of();
            List<StructureLibHatchDescriptionLine> copied = new ArrayList<>(lines.size());
            for (StructureLibHatchDescriptionLine line : lines) {
                if (line != null) copied.add(line);
            }
            return copied.isEmpty() ? List.of() : List.copyOf(copied);
        }
    }

    @Getter
    public static class TierData {

        private final int minValue;
        private final int maxValue;
        private final int defaultValue;
        private final int currentValue;

        public TierData(int minValue, int maxValue, int defaultValue, int currentValue) {
            int normalizedMin = Math.max(1, minValue);
            int normalizedMax = Math.max(normalizedMin, maxValue);
            this.minValue = normalizedMin;
            this.maxValue = normalizedMax;
            this.defaultValue = clamp(defaultValue, normalizedMin, normalizedMax);
            this.currentValue = clamp(currentValue, normalizedMin, normalizedMax);
        }

        public boolean isSelectable() {
            return maxValue > minValue;
        }
    }

    @Getter
    public static class ChannelData {

        private final String channelId;
        private final String label;
        private final int maxValue;
        private final int defaultValue;
        private final int currentValue;

        public ChannelData(String channelId, String label, int maxValue, int defaultValue, int currentValue) {
            String normalizedChannelId = StructureLibPreviewSelection.normalizeChannelId(channelId);
            String normalizedLabel = normalizeOptional(label);
            int normalizedMax = Math.max(0, maxValue);
            this.channelId = normalizedChannelId != null ? normalizedChannelId : "channel";
            this.label = normalizedLabel != null ? normalizedLabel : this.channelId;
            this.maxValue = normalizedMax;
            this.defaultValue = clamp(defaultValue, 0, normalizedMax);
            this.currentValue = clamp(currentValue, 0, normalizedMax);
        }

        public int getMinValue() {
            return 0;
        }

        public boolean isSelectable() {
            return maxValue > 0;
        }
    }
}
