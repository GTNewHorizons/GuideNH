package com.hfstudio.guidenh.client.capture.api;

import java.util.Set;

public record CaptureSnapOptions(boolean enabled, Set<String> targetKinds, double acquireDistance,
    double releaseDistance, boolean showClassName) {

    public CaptureSnapOptions {
        targetKinds = Set.copyOf(targetKinds);
    }

    public boolean accepts(CaptureTarget target) {
        return enabled && targetKinds.contains(target.kind())
            && !target.visibleBounds()
                .isEmpty();
    }
}
