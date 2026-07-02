package com.hfstudio.guidenh.bridge.preview;

import java.util.List;

import lombok.Getter;

@Getter
public class PreviewResolveResult {

    private final String capability;
    private final String previewKey;
    private final String id;
    private final String displayName;
    private final String detail;
    private final Integer meta;
    private final Integer count;
    private final String nbt;
    private final List<String> tooltipLines;
    private final String iconPngBase64;
    private final int pixelWidth;
    private final int pixelHeight;

    public PreviewResolveResult(String capability, String previewKey, String id, String displayName, String detail,
        Integer meta, Integer count, String nbt, List<String> tooltipLines, String iconPngBase64, int pixelWidth,
        int pixelHeight) {
        this.capability = capability == null ? "" : capability;
        this.previewKey = previewKey == null ? "" : previewKey;
        this.id = id == null ? "" : id;
        this.displayName = displayName;
        this.detail = detail;
        this.meta = meta;
        this.count = count;
        this.nbt = nbt;
        this.tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
        this.iconPngBase64 = iconPngBase64 == null ? "" : iconPngBase64;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
    }

}
