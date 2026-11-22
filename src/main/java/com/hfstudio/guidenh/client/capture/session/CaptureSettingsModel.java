package com.hfstudio.guidenh.client.capture.session;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureSnapOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.config.CaptureConfig;

public class CaptureSettingsModel {

    private final CaptureConfig config;
    private int requestedWidth;
    private int requestedHeight;
    private boolean keepAspectRatio = true;

    public CaptureSettingsModel(CaptureConfig config) {
        this.config = config;
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
        CaptureContentOptions content = new CaptureContentOptions(
            config.includeWorld,
            config.includeGuiBackdrop,
            config.includeTooltips,
            config.includeDebug,
            config.includeNei,
            config.backgroundArgb);
        CaptureLayoutOptions layout = new CaptureLayoutOptions(config.arrangement, config.columns, config.spacing);
        Path output = Path.of(config.outputDirectory);
        if (!output.isAbsolute()) output = gameDirectory.resolve(output);
        return new CaptureOptions(
            resolution,
            content,
            layout,
            output.normalize(),
            (long) config.temporaryBudgetMiB << 20);
    }

    public CaptureSnapOptions snapping(boolean temporarilyDisabled) {
        Set<String> kinds = new HashSet<>();
        if (config.snapSlots) kinds.add(CaptureTarget.SLOT);
        if (config.snapComponents) kinds.add(CaptureTarget.COMPONENT);
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
