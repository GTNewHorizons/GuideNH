package com.hfstudio.guidenh.client.capture.session;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.hfstudio.guidenh.client.capture.api.CaptureBackendSelection;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureFrameIdentity;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureSessionState;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureExportWorker;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;
import com.hfstudio.guidenh.client.capture.recording.CaptureRecording;
import com.hfstudio.guidenh.config.ModConfig;

public class CaptureSession implements AutoCloseable {

    private final UUID id = UUID.randomUUID();
    private GuiScreen sourceScreen;
    private final CaptureSelectionController selections;
    private final CaptureSettingsModel settings;
    private CaptureViewport viewport;
    private CaptureSessionState state = CaptureSessionState.PREPARING;
    private CaptureFrame preview;
    private CaptureFrameIdentity previewIdentity;
    private long previewVersion;
    private boolean refresh = true;
    private long lastPreviewTime;
    private CompletableFuture<CaptureOutputArtifact> export;
    private CaptureOutputArtifact lastArtifact;
    private String message = "";
    private String outputQuality = "native";
    private CaptureRecording recording;
    private boolean previewRequested;
    private boolean keepOpenAfterSave;
    private boolean targetMissing;

    public CaptureSession(GuiScreen sourceScreen, CaptureViewport viewport) {
        this.sourceScreen = sourceScreen;
        this.viewport = viewport;
        selections = new CaptureSelectionController(viewport.bounds());
        settings = new CaptureSettingsModel(ModConfig.capture);
    }

    public GuiScreen sourceScreen() {
        return sourceScreen;
    }

    public void adoptScreen(GuiScreen screen, CaptureViewport viewport) {
        if (sourceScreen == screen) return;
        sourceScreen = screen;
        this.viewport = viewport;
        selections.setBounds(viewport.bounds());
        if (preview != null) preview.close();
        preview = null;
        previewIdentity = null;
        requestRefresh();
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

    public CaptureFrameIdentity previewIdentity() {
        return previewIdentity;
    }

    public boolean previewMatches(CaptureContentOptions content, CaptureBackendSelection backend) {
        return preview != null && previewIdentity != null
            && previewIdentity.content()
                .equals(content)
            && previewIdentity.requestedBackend()
                .equals(backend);
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
        if (recording != null && !recording.finished() && !recording.paused()) pauseRecording();
        selections.setBounds(current.bounds());
        requestRefresh();
    }

    public void requestRefresh() {
        refresh = true;
    }

    public void invalidatePreview() {
        if (preview != null) preview.close();
        preview = null;
        previewIdentity = null;
        previewVersion++;
        requestRefresh();
    }

    public void follow(Function<String, CaptureTarget> resolve) {
        targetMissing = !selections.follow(resolve)
            .isEmpty();
        if (targetMissing) {
            if (recording != null && !recording.finished() && !recording.paused()) pauseRecording();
            message = I18n.format("guidenh.capture.follow_missing");
        } else if (selections.following() && recording != null
            && !recording.finished()
            && !recording.paused()
            && !recordingSizeMatches()) {
                pauseRecording();
                message = I18n.format("guidenh.capture.follow_resized");
            }
    }

    private boolean recordingSizeMatches() {
        CaptureComposition initial = CaptureRowComposer.compose(recording.request());
        CaptureComposition current = CaptureRowComposer.compose(
            recording.request()
                .withRegions(selections.regions()));
        CaptureOutputSize initialSize = recording.request()
            .options()
            .resolution()
            .resolve(
                initial.size()
                    .width(),
                initial.size()
                    .height());
        CaptureOutputSize currentSize = recording.request()
            .options()
            .resolution()
            .resolve(
                current.size()
                    .width(),
                current.size()
                    .height());
        return initialSize.equals(currentSize);
    }

    public boolean wantsFrame(long now) {
        previewRequested = preview == null || refresh
            || (!settings.config().freezePreview || selections.following()) && now - lastPreviewTime >= 100_000_000L;
        if (recording != null) {
            recording.tick(now);
            if (recording.finished() && !state.equals(CaptureSessionState.SAVING)) state = CaptureSessionState.SAVING;
        }
        return previewRequested || recording != null && recording.needsFrame(now);
    }

    public boolean previewRequested() {
        return previewRequested;
    }

    public void acceptFrame(CaptureFrame frame) {
        acceptFrame(
            frame,
            new CaptureFrameIdentity(CaptureBackendSelection.automatic(), settings.backend(), settings.content()));
    }

    public void acceptFrame(CaptureFrame frame, CaptureFrameIdentity identity) {
        if (recording != null) recording.offer(frame, System.nanoTime(), selections.regions());
        if (!previewRequested) {
            frame.close();
            return;
        }
        if (preview != null) preview.close();
        preview = frame;
        previewIdentity = identity;
        previewVersion++;
        lastPreviewTime = System.nanoTime();
        refresh = false;
        if (state.equals(CaptureSessionState.PREPARING)) state = CaptureSessionState.EDITING;
    }

    public boolean canSave() {
        return previewMatches(settings.content(), settings.backend()) && selections.regions()
            .stream()
            .anyMatch(
                region -> !region.erased() && !region.bounds()
                    .isEmpty())
            && !selections.dragging()
            && !(targetMissing && selections.following())
            && !busy();
    }

    public boolean busy() {
        return state.equals(CaptureSessionState.SAVING) || state.equals(CaptureSessionState.RECORDING)
            || state.equals(CaptureSessionState.PAUSED);
    }

    public CaptureRecording recording() {
        return recording;
    }

    public void startRecording(CaptureExportWorker worker, Path gameDirectory) {
        if (!canSave()) return;
        CaptureRecording created = new CaptureRecording(
            request(gameDirectory),
            settings.recording(),
            System.nanoTime());
        try {
            worker.startRecording(created);
        } catch (RuntimeException error) {
            created.close();
            throw error;
        }
        recording = created;
        export = recording.completion();
        state = CaptureSessionState.RECORDING;
        outputQuality = settings.config().scale > 1 || settings.requestedWidth() > 0 || settings.requestedHeight() > 0
            ? "resampled"
            : "native";
        message = I18n.format("guidenh.capture.recording_started");
    }

    public void pauseRecording() {
        if (recording == null || recording.finished()) return;
        if (recording.paused() && (targetMissing || !recordingSizeMatches())) {
            message = I18n.format(targetMissing ? "guidenh.capture.follow_missing" : "guidenh.capture.follow_resized");
            return;
        }
        if (recording.paused() && !viewport.equals(
            recording.request()
                .viewport())) {
            message = I18n.format("guidenh.capture.target_changed");
            return;
        }
        recording.togglePause(System.nanoTime());
        state = recording.paused() ? CaptureSessionState.PAUSED : CaptureSessionState.RECORDING;
    }

    public void stopRecording() {
        if (recording == null || recording.finished()) return;
        recording.stop(System.nanoTime());
        state = CaptureSessionState.SAVING;
        message = I18n.format("guidenh.capture.saving");
    }

    public CaptureExportRequest request(Path gameDirectory) {
        return new CaptureExportRequest(
            id,
            sourceScreen.getClass()
                .getSimpleName(),
            viewport,
            selections.regions(),
            settings.snapshot(gameDirectory)
                .withBackend(previewIdentity == null ? settings.backend() : previewIdentity.backend()));
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

    public void keepOpenAfterSave(boolean keepOpen) {
        keepOpenAfterSave = keepOpen;
    }

    public void outputQuality(String quality) {
        outputQuality = quality;
    }

    public void pollExport() {
        if (recording != null) {
            recording.tick(System.nanoTime());
            if (recording.finished()) state = CaptureSessionState.SAVING;
        }
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
            state = settings.config().closeAfterSave && !keepOpenAfterSave ? CaptureSessionState.CLOSED
                : CaptureSessionState.EDITING;
        } catch (CancellationException error) {
            state = CaptureSessionState.EDITING;
            message = I18n.format("guidenh.capture.recording_cancelled");
        } catch (CompletionException error) {
            state = CaptureSessionState.EDITING;
            message = I18n.format(
                "guidenh.capture.failed",
                error.getCause()
                    .getMessage());
        }
        export = null;
        recording = null;
    }

    @Override
    public void close() {
        state = CaptureSessionState.CLOSED;
        if (export != null) export.cancel(true);
        if (recording != null) recording.close();
        if (preview != null) preview.close();
        preview = null;
        previewIdentity = null;
        ModConfig.save();
    }
}
