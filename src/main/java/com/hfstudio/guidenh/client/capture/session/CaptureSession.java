package com.hfstudio.guidenh.client.capture.session;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.config.ModConfig;

public class CaptureSession implements AutoCloseable {

    private final UUID id = UUID.randomUUID();
    private final GuiScreen sourceScreen;
    private final CaptureSelectionController selections;
    private final CaptureSettingsModel settings;
    private CaptureViewport viewport;
    private CaptureSessionState state = CaptureSessionState.PREPARING;
    private CaptureFrame preview;
    private long previewVersion;
    private boolean refresh = true;
    private long lastPreviewTime;
    private CompletableFuture<CaptureOutputArtifact> export;
    private CaptureOutputArtifact lastArtifact;
    private String message = "";
    private String outputQuality = "native";

    public CaptureSession(GuiScreen sourceScreen, CaptureViewport viewport) {
        this.sourceScreen = sourceScreen;
        this.viewport = viewport;
        selections = new CaptureSelectionController(viewport.bounds());
        settings = new CaptureSettingsModel(ModConfig.capture);
    }

    public GuiScreen sourceScreen() {
        return sourceScreen;
    }

    public CaptureViewport viewport() {
        return viewport;
    }

    public CaptureSelectionController selections() {
        return selections;
    }

    public CaptureSettingsModel settings() {
        return settings;
    }

    public CaptureSessionState state() {
        return state;
    }

    public CaptureFrame preview() {
        return preview;
    }

    public long previewVersion() {
        return previewVersion;
    }

    public CaptureOutputArtifact lastArtifact() {
        return lastArtifact;
    }

    public String message() {
        return message;
    }

    public void notify(String text) {
        message = text;
    }

    public void updateViewport(CaptureViewport current) {
        if (viewport.equals(current)) return;
        viewport = current;
        selections.setBounds(current.bounds());
        requestRefresh();
    }

    public void requestRefresh() {
        refresh = true;
    }

    public boolean wantsFrame(long now) {
        return preview == null || refresh || !settings.config().freezePreview && now - lastPreviewTime >= 100_000_000L;
    }

    public void acceptFrame(CaptureFrame frame) {
        if (preview != null) preview.close();
        preview = frame;
        previewVersion++;
        lastPreviewTime = System.nanoTime();
        refresh = false;
        if (state.equals(CaptureSessionState.PREPARING)) state = CaptureSessionState.EDITING;
    }

    public boolean canSave() {
        return preview != null && !selections.regions()
            .isEmpty() && !selections.dragging() && !state.equals(CaptureSessionState.SAVING);
    }

    public CaptureExportRequest request(Path gameDirectory) {
        return new CaptureExportRequest(
            id,
            sourceScreen.getClass()
                .getSimpleName(),
            viewport,
            selections.regions(),
            settings.snapshot(gameDirectory));
    }

    public void save(CaptureExportWorker worker, Path gameDirectory) {
        if (!canSave()) return;
        beginExport(worker.submit(request(gameDirectory), preview, settings.config().format));
    }

    public void beginExport(CompletableFuture<CaptureOutputArtifact> task) {
        export = task;
        state = CaptureSessionState.SAVING;
        message = I18n.format("guidenh.capture.saving");
    }

    public void preparingOutput() {
        state = CaptureSessionState.SAVING;
    }

    public void outputFailed(Exception error) {
        state = CaptureSessionState.EDITING;
        message = I18n.format("guidenh.capture.failed", error.getMessage());
    }

    public void outputQuality(String quality) {
        outputQuality = quality;
    }

    public void pollExport() {
        if (export == null || !export.isDone()) return;
        try {
            lastArtifact = export.join();
            message = I18n.format(
                "guidenh.capture.saved",
                lastArtifact.path()
                    .getFileName()
                    .toString())
                + " · "
                + I18n.format("guidenh.capture.quality." + outputQuality);
            state = settings.config().closeAfterSave ? CaptureSessionState.CLOSED : CaptureSessionState.EDITING;
        } catch (CompletionException error) {
            state = CaptureSessionState.EDITING;
            message = I18n.format(
                "guidenh.capture.failed",
                error.getCause()
                    .getMessage());
        }
        export = null;
    }

    @Override
    public void close() {
        state = CaptureSessionState.CLOSED;
        if (preview != null) preview.close();
        preview = null;
        ModConfig.save();
    }
}
