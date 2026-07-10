package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class StructureLibImportResult {

    @Getter
    private final boolean success;
    @Getter
    private final List<PlacedBlock> blocks;
    @Getter
    private final List<String> warnings;
    @Getter
    private final List<String> errors;
    @Nullable
    private final StructureLibSceneMetadata metadata;

    public StructureLibImportResult(boolean success, List<PlacedBlock> blocks, List<String> warnings,
        List<String> errors, @Nullable StructureLibSceneMetadata metadata) {
        this(success, immutableCopy(blocks), immutableCopy(warnings), immutableCopy(errors), metadata, true);
    }

    private StructureLibImportResult(boolean success, List<PlacedBlock> blocks, List<String> warnings,
        List<String> errors, @Nullable StructureLibSceneMetadata metadata, boolean reuseImmutableLists) {
        this.success = success;
        this.blocks = reuseImmutableLists ? blocks : immutableCopy(blocks);
        this.warnings = reuseImmutableLists ? warnings : immutableCopy(warnings);
        this.errors = reuseImmutableLists ? errors : immutableCopy(errors);
        this.metadata = metadata;
    }

    public static StructureLibImportResult success(List<PlacedBlock> blocks, List<String> warnings,
        @Nullable StructureLibSceneMetadata metadata) {
        return new StructureLibImportResult(true, blocks, warnings, List.of(), metadata);
    }

    public static StructureLibImportResult failure(String error) {
        return failure(error, List.of(), null);
    }

    public static StructureLibImportResult failure(String error, List<String> warnings,
        @Nullable StructureLibSceneMetadata metadata) {
        String normalized = normalizeMessage(error);
        return new StructureLibImportResult(false, List.of(), warnings, List.of(normalized), metadata);
    }

    public StructureLibImportResult withWarnings(List<String> nextWarnings) {
        return new StructureLibImportResult(success, blocks, immutableCopy(nextWarnings), errors, metadata, true);
    }

    @Nullable
    public StructureLibSceneMetadata getMetadata() {
        return metadata;
    }

    public static <T> List<T> immutableCopy(@Nullable List<T> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source);
    }

    public static String normalizeMessage(@Nullable String message) {
        if (message == null) {
            return "Unknown StructureLib import error";
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? "Unknown StructureLib import error" : trimmed;
    }

    public static class PlacedBlock {

        @Getter
        private final int x;
        @Getter
        private final int y;
        @Getter
        private final int z;
        @Getter
        private final Block block;
        @Getter
        private final int meta;
        @Nullable
        private final NBTTagCompound tileTag;
        @Nullable
        private final String blockId;

        public PlacedBlock(int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound tileTag,
            @Nullable String blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.tileTag = tileTag != null ? (NBTTagCompound) tileTag.copy() : null;
            this.blockId = normalizeBlockId(blockId);
        }

        @Nullable
        public NBTTagCompound getTileTag() {
            return tileTag;
        }

        @Nullable
        public String getBlockId() {
            return blockId;
        }

        @Nullable
        public static String normalizeBlockId(@Nullable String blockId) {
            if (blockId == null) {
                return null;
            }
            String trimmed = blockId.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
