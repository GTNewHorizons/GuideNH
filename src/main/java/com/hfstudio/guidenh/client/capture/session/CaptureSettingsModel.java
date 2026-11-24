package com.hfstudio.guidenh.client.capture.session;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.config.CaptureConfig;
import com.hfstudio.guidenh.network.capture.CaptureCapabilityMessage;

public class CaptureSettingsModel {

    private final CaptureConfig config;
    private boolean gameplay;
    private int requestedWidth;
    private int requestedHeight;
    private boolean keepAspectRatio = true;

    public CaptureSettingsModel(CaptureConfig config) {
        this(config, false);
    }

    public CaptureSettingsModel(CaptureConfig config, boolean gameplay) {
        this.config = config;
        this.gameplay = gameplay;
    }

    public boolean gameplay() {
        return gameplay;
    }

    public void gameplay(boolean value) {
        gameplay = value;
    }

    public boolean includeHud() {
        return gameplay ? config.includeGameplayHud : config.includeHud;
    }

    public void includeHud(boolean value) {
        if (gameplay) config.includeGameplayHud = value;
        else config.includeHud = value;
    }

    public CaptureConfig config() {
        return config;
    }

    public int requestedWidth() {
        return requestedWidth;
    }

    public int requestedHeight() {
        return requestedHeight;
    }

    public void setRequestedWidth(int width) {
        if (width < 0) throw new IllegalArgumentException("Width cannot be negative");
        requestedWidth = width;
        if (keepAspectRatio) requestedHeight = 0;
    }

    public void setRequestedHeight(int height) {
        if (height < 0) throw new IllegalArgumentException("Height cannot be negative");
        requestedHeight = height;
        if (keepAspectRatio) requestedWidth = 0;
    }

    public boolean keepAspectRatio() {
        return keepAspectRatio;
    }

    public void setKeepAspectRatio(boolean keep) {
        keepAspectRatio = keep;
    }

    public CaptureOptions snapshot(Path gameDirectory) {
        CaptureResolution resolution = new CaptureResolution(
            config.scale,
            config.scale,
            requestedWidth,
            requestedHeight,
            keepAspectRatio,
            config.qualityPolicy);
        CaptureContentOptions content = content();
        CaptureLayoutOptions layout = new CaptureLayoutOptions(
            config.arrangement,
            config.columns,
            config.spacing,
            config.separateRegions);
        Path output = Path.of(config.outputDirectory);
        if (!output.isAbsolute()) output = gameDirectory.resolve(output);
        return new CaptureOptions(
            resolution,
            content,
            layout,
            output.normalize(),
            (long) config.temporaryBudgetMiB << 20,
            backend());
    }

    public CaptureBackendSelection backend() {
        return new CaptureBackendSelection(config.sourceId, config.readerId);
    }

    public CaptureContentOptions content() {
        return new CaptureContentOptions(
            config.includeWorld,
            config.includeGuiBackdrop,
            config.includeTooltips,
            config.includeDebug,
            config.includeNei,
            config.backgroundArgb,
            includeHud(),
            config.includeCursor,
            config.includeGui,
            config.includePlayerInventory);
    }

    public CaptureBatchOptions batch(Path gameDirectory) {
        Set<String> scopes = new HashSet<>();
        if (config.captureVisibleLayers) scopes.add(CaptureBatchOptions.VISIBLE);
        if (config.capturePages) scopes.add(CaptureBatchOptions.PAGES);
        if (config.captureReachableGuis) scopes.add(CaptureBatchOptions.REACHABLE);
        Set<String> providers = Arrays.stream(config.batchProviders.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toSet());
        return new CaptureBatchOptions(
            scopes,
            providers,
            config.batchVisibleMode,
            config.includePopups,
            config.includeVariants,
            config.stableFrames,
            config.timeoutSeconds,
            config.maxTargets,
            config.restoreOriginal,
            config.tileSize,
            (long) config.memoryBudgetMiB << 20,
            snapshot(gameDirectory),
            config.format.equals("png") ? null : recording(),
            new CaptureServerQuery(
                config.serverCaptureRadius,
                Math.min(config.maxTargets, CaptureCapabilityMessage.MAX_TARGETS),
                config.serverTemporaryTravel),
            new CaptureBatchLayoutOptions(
                config.batchContactSheet,
                config.batchGroupByProvider,
                config.batchSheetColumns,
                config.batchThumbnailSize,
                config.batchSheetPageSize,
                config.batchScrollImages));
    }

    public CaptureRecordingOptions recording() {
        return new CaptureRecordingOptions(
            config.format.equals("png") ? "gif" : config.format,
            config.fps,
            config.recordingSeconds,
            config.frameLimit,
            config.loopCount,
            config.gifAlphaThreshold,
            config.gifMatte,
            config.gifDither,
            config.spriteColumns,
            config.spritePadding,
            config.spritePageSize,
            config.queueFrames,
            (long) config.memoryBudgetMiB << 20,
            config.strictRecordingQueue);
    }

    public CaptureSnapOptions snapping(boolean temporarilyDisabled) {
        Set<String> kinds = new HashSet<>();
        if (config.snapSlots) kinds.add(CaptureTarget.SLOT);
        if (config.snapComponents) {
            kinds.add(CaptureTarget.COMPONENT);
            kinds.add(CaptureTarget.TOOLTIP);
        }
        if (config.snapWindows) {
            kinds.add(CaptureTarget.WINDOW);
            kinds.add(CaptureTarget.SCREEN);
        }
        return new CaptureSnapOptions(
            config.snapping && !temporarilyDisabled,
            kinds,
            config.snapDistance,
            config.snapReleaseDistance,
            config.showClassName);
    }
}
