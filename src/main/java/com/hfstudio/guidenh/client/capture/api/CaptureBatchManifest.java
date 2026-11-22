package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.UUID;

public record CaptureBatchManifest(UUID id, int maxTargets, String restoration, String restorationReason,
    List<CaptureBatchResult> results) {

    public CaptureBatchManifest {
        results = List.copyOf(results);
    }
}
