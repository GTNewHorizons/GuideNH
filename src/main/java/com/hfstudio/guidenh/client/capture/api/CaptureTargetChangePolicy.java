package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public enum CaptureTargetChangePolicy {

    PAUSE("pause"),
    FINISH_SEGMENT("finish_segment"),
    NEXT_SEGMENT("next_segment");

    private static final List<CaptureTargetChangePolicy> POLICIES = List.of(values());
    private static final List<String> IDS = POLICIES.stream()
        .map(CaptureTargetChangePolicy::id)
        .toList();
    private final String id;

    private CaptureTargetChangePolicy(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static List<String> ids() {
        return IDS;
    }

    public static CaptureTargetChangePolicy byId(String id) {
        for (CaptureTargetChangePolicy policy : POLICIES) if (policy.id.equals(id)) return policy;
        throw new IllegalArgumentException("Unknown recording target change policy: " + id);
    }
}
