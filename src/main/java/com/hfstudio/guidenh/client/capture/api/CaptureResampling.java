package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public enum CaptureResampling {

    NEAREST("nearest"),
    SMOOTH("smooth");

    private static final List<CaptureResampling> MODES = List.of(values());
    private static final List<String> IDS = MODES.stream()
        .map(CaptureResampling::id)
        .toList();
    private final String id;

    private CaptureResampling(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static List<String> ids() {
        return IDS;
    }

    public static CaptureResampling byId(String id) {
        for (CaptureResampling mode : MODES) {
            if (mode.id.equals(id)) return mode;
        }
        throw new IllegalArgumentException("Unknown capture resampling mode: " + id);
    }
}
