package com.hfstudio.guidenh.bridge.preview;

import java.util.Objects;

import lombok.Getter;

@Getter
public class ItemPreviewCacheKey {

    private final String capability;
    private final String id;
    private final int meta;
    private final int count;
    private final String nbt;
    private final String renderVariant;

    public ItemPreviewCacheKey(String capability, String id, int meta, int count, String nbt, String renderVariant) {
        this.capability = capability == null ? "" : capability;
        this.id = id == null ? "" : id;
        this.meta = meta;
        this.count = count;
        this.nbt = nbt == null ? "" : nbt;
        this.renderVariant = renderVariant == null ? "default" : renderVariant;
    }

    public String toPreviewKey() {
        return capability + "|"
            + id
            + "|"
            + meta
            + "|"
            + count
            + "|"
            + renderVariant
            + "|"
            + Integer.toHexString(nbt.hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemPreviewCacheKey that)) {
            return false;
        }
        return meta == that.meta && count == that.count
            && Objects.equals(capability, that.capability)
            && Objects.equals(id, that.id)
            && Objects.equals(nbt, that.nbt)
            && Objects.equals(renderVariant, that.renderVariant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capability, id, meta, count, nbt, renderVariant);
    }
}
