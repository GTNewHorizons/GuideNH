package com.hfstudio.guidenh.bridge.preview;

import lombok.Getter;

@Getter
public class PreviewSearchEntry {

    private final String id;
    private final String label;
    private final String detail;
    private final String previewKey;
    private final String matchKind;

    public PreviewSearchEntry(String id, String label, String detail, String previewKey, String matchKind) {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.previewKey = previewKey;
        this.matchKind = matchKind;
    }

}
