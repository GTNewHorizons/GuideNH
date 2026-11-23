package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.UUID;

public record CaptureBatchManifest(UUID id, int maxTargets, String restoration, String restorationReason,
    List<CaptureBatchResult> results, List<CaptureBatchOutput> outputs) {

    public CaptureBatchManifest(UUID id, int maxTargets, String restoration, String restorationReason,
        List<CaptureBatchResult> results) {
        this(id, maxTargets, restoration, restorationReason, results, List.of());
    }

    public CaptureBatchManifest {
        results = List.copyOf(results);
        outputs = List.copyOf(outputs);
    }
}
