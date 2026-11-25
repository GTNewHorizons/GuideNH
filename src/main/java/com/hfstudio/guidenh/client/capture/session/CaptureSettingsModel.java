package com.hfstudio.guidenh.client.capture.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.capture.server.api.CaptureServerArea;
import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.client.capture.api.CaptureAlignment;
import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureFormatCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureFormatSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureResampling;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureScheduleCondition;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetChangePolicy;
import com.hfstudio.guidenh.config.CaptureConfig;
import com.hfstudio.guidenh.network.capture.CaptureCapabilityMessage;

public class CaptureSettingsModel {

    private final CaptureConfig config;
    private final CaptureFormatCatalog formats;
    private boolean gameplay;
    private int requestedWidth;
    private int requestedHeight;
    private boolean keepAspectRatio = true;

    public CaptureSettingsModel(CaptureConfig config) {
        this(config, false);
    }

    public CaptureSettingsModel(CaptureConfig config, boolean gameplay) {
        this(config, gameplay, CaptureFormatSnapshot.STANDARD);
    }

    public CaptureSettingsModel(CaptureConfig config, boolean gameplay, CaptureFormatCatalog formats) {
        this.config = Objects.requireNonNull(config);
        this.gameplay = gameplay;
        this.formats = Objects.requireNonNull(formats);
    }

    public List<String> formats() {
        return List.copyOf(formats.formats());
    }

    public boolean animationFormat() {
        return formats.animationFormat(config.format);
    }

    public List<String> pageAnimationFormats() {
        List<String> choices = new ArrayList<>();
        choices.add("none");
        formats().stream()
            .filter(formats::animationFormat)
            .forEach(choices::add);
        return List.copyOf(choices);
    }

    public void setPageAnimationFormat(String format) {
        if (!pageAnimationFormats().contains(format))
            throw new IllegalArgumentException("Unknown page animation format: " + format);
        config.batchPageFormat = format;
    }

    public void setFormat(String format) {
        requireFormat(format);
        config.format = format;
    }

    private void requireFormat(String format) {
        if (!formats().contains(format)) throw new IllegalArgumentException("Unknown capture format: " + format);
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
        requireFormat(config.format);
        CaptureResolution resolution = new CaptureResolution(
            config.scale,
            config.scale,
            requestedWidth,
            requestedHeight,
            keepAspectRatio,
            config.qualityPolicy,
            CaptureResampling.byId(config.resampling));
        CaptureContentOptions content = content();
        CaptureLayoutOptions layout = new CaptureLayoutOptions(
            config.arrangement,
            config.columns,
            config.spacing,
            config.separateRegions,
            CaptureAlignment.byId(config.alignment));
        Path output = Path.of(config.outputDirectory);
        if (!output.isAbsolute()) output = gameDirectory.resolve(output);
        return new CaptureOptions(
            resolution,
            content,
            layout,
            output.normalize(),
            (long) config.temporaryBudgetMiB << 20,
            backend(),
            config.format);
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
            animationFormat() ? recording() : null,
            new CaptureServerQuery(
                config.serverCaptureRadius,
                Math.min(config.maxTargets, CaptureCapabilityMessage.MAX_TARGETS),
                config.serverTemporaryTravel,
                CaptureServerArea.parse(
                    config.serverCaptureArea,
                    Minecraft.getMinecraft().thePlayer == null ? 0 : Minecraft.getMinecraft().thePlayer.dimension)),
            new CaptureBatchLayoutOptions(
                config.batchContactSheet,
                config.batchGroupByProvider,
                config.batchSheetColumns,
                config.batchThumbnailSize,
                config.batchSheetPageSize,
                config.batchScrollImages,
                config.batchPageFormat.equals("none") ? null : recording(config.batchPageFormat),
                config.batchPageDelayMillis,
                config.batchPageMaximumDimension));
    }

    public CaptureRecordingOptions recording() {
        return recording(animationFormat() ? config.format : "gif");
    }

    public CaptureRecordingOptions recording(String format) {
        requireFormat(format);
        if (!formats.animationFormat(format))
            throw new IllegalArgumentException("This capture format does not support animation: " + format);
        return new CaptureRecordingOptions(
            format,
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
            config.strictRecordingQueue,
            CaptureTargetChangePolicy.byId(config.recordingTargetChangePolicy));
    }

    public String schedulePolicy(CaptureScheduleCondition condition) {
        String policy = configuredSchedulePolicy(condition);
        return policy.equals("inherit") ? config.unavailablePolicy : policy;
    }

    public String configuredSchedulePolicy(CaptureScheduleCondition condition) {
        return switch (condition) {
            case FOCUS -> config.scheduleFocusPolicy;
            case MINIMIZED -> config.scheduleMinimizedPolicy;
            case NO_GUI -> config.scheduleNoGuiPolicy;
            case PAUSED -> config.schedulePausedPolicy;
            case TARGET -> config.scheduleTargetPolicy;
        };
    }

    public void setSchedulePolicy(CaptureScheduleCondition condition, String policy) {
        if (!List.of("inherit", "wait", "skip", "stop")
            .contains(policy)) throw new IllegalArgumentException("Unknown scheduled capture policy: " + policy);
        switch (condition) {
            case FOCUS -> config.scheduleFocusPolicy = policy;
            case MINIMIZED -> config.scheduleMinimizedPolicy = policy;
            case NO_GUI -> config.scheduleNoGuiPolicy = policy;
            case PAUSED -> config.schedulePausedPolicy = policy;
            case TARGET -> config.scheduleTargetPolicy = policy;
        }
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
