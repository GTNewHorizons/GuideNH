package com.hfstudio.guidenh.client.capture.api;

import java.util.Set;

public record CaptureSnapOptions(boolean enabled, Set<String> targetKinds, double acquireDistance,
    double releaseDistance, boolean showClassName, Set<String> disabledProviders) {

    public CaptureSnapOptions(boolean enabled, Set<String> targetKinds, double acquireDistance, double releaseDistance,
        boolean showClassName) {
        this(enabled, targetKinds, acquireDistance, releaseDistance, showClassName, Set.of());
    }

    public CaptureSnapOptions {
        targetKinds = Set.copyOf(targetKinds);
        disabledProviders = disabledProviders == null ? Set.of() : Set.copyOf(disabledProviders);
    }

    public boolean accepts(CaptureTarget target) {
        return enabled && targetKinds.contains(target.kind())
            && !disabledProviders.contains(target.providerId())
            && !target.visibleBounds()
                .isEmpty();
    }
}
