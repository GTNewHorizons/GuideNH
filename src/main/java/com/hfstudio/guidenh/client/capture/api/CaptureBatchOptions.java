package com.hfstudio.guidenh.client.capture.api;

import java.util.Set;

import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;

public record CaptureBatchOptions(Set<String> scopes, Set<String> providerIds, String visibleMode,
    boolean includePopups, boolean includeVariants, int stableFrames, int timeoutSeconds, int maxTargets,
    boolean restoreOriginal, int tileSize, long memoryBudgetBytes, CaptureOptions output,
    CaptureRecordingOptions recording, CaptureServerQuery serverQuery, CaptureBatchLayoutOptions layout) {

    public CaptureBatchOptions(Set<String> scopes, Set<String> providerIds, String visibleMode, boolean includePopups,
        boolean includeVariants, int stableFrames, int timeoutSeconds, int maxTargets, boolean restoreOriginal,
        int tileSize, long memoryBudgetBytes, CaptureOptions output, CaptureRecordingOptions recording,
        CaptureServerQuery serverQuery) {
        this(
            scopes,
            providerIds,
            visibleMode,
            includePopups,
            includeVariants,
            stableFrames,
            timeoutSeconds,
            maxTargets,
            restoreOriginal,
            tileSize,
            memoryBudgetBytes,
            output,
            recording,
            serverQuery,
            CaptureBatchLayoutOptions.filesOnly());
    }

    public static final String VISIBLE = "visible";
    public static final String PAGES = "pages";
    public static final String REACHABLE = "reachable";

    public CaptureBatchOptions {
        scopes = Set.copyOf(scopes);
        providerIds = Set.copyOf(providerIds);
    }
}
