package com.hfstudio.guidenh.client.capture.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.hfstudio.guidenh.client.capture.CaptureRegistry;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchController;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchHost;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProgress;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.client.capture.export.CaptureOutputPaths;

public class CaptureBatchEngine implements CaptureBatchController, AutoCloseable {

    public enum Stage {
        IDLE,
        SELECTING,
        WAITING,
        SAVING,
        RESTORING,
        REPORTING,
        COMPLETE
    }

    private final CaptureRegistry registry;
    private final CaptureBatchHost host;
    private final Map<String, CaptureBatchNavigator> navigators = new LinkedHashMap<>();
    private final Map<String, CaptureBatchResult> results = new LinkedHashMap<>();
    private CaptureBatchCatalog catalog;
    private List<CaptureTargetDescriptor> selected = List.of();
    private Stage stage = Stage.IDLE;
    private UUID id;
    private Path directory;
    private CaptureFrame visibleFrame;
    private CaptureTargetDescriptor current;
    private CompletableFuture<CaptureOutputArtifact> capture;
    private CompletableFuture<Path> report;
    private long deadline;
    private long lastFrame = Long.MIN_VALUE;
    private long lastRevision = Long.MIN_VALUE;
    private int stableFrames;
    private int nextTarget;
    private int attempted;
    private boolean cancelRequested;
    private String reason = "";
    private String restoration = "not_requested";
    private String restorationReason = "";

    public CaptureBatchEngine(CaptureRegistry registry, CaptureBatchHost host) {
        this.registry = registry;
        this.host = host;
    }

    @Override
    public CaptureBatchCatalog catalog() {
        return catalog;
    }

    @Override
    public void refresh() {
        if (busy()) return;
        catalog = registry.catalog(host.context(), host.options());
        reason = "";
    }

    @Override
    public void start(Set<String> targetIds) {
        if (busy()) return;
        if (!host.readyForBatch()) {
            reason = "Finish the active capture before starting a batch";
            return;
        }
        if (catalog == null) refresh();
        if (host.frame() == null) {
            reason = "The preview is still preparing";
            return;
        }
        close();
        results.clear();
        selected = catalog.targets()
            .stream()
            .filter(target -> targetIds.contains(target.id()))
            .toList();
        if (selected.isEmpty()) {
            reason = "Choose at least one target";
            return;
        }
        id = UUID.randomUUID();
        nextTarget = attempted = 0;
        cancelRequested = false;
        restoration = "not_requested";
        restorationReason = reason = "";
        visibleFrame = host.frame()
            .retain();
        try {
            directory = CaptureOutputPaths.next(
                catalog.options()
                    .output()
                    .outputDirectory(),
                "gui_batch",
                "batch");
            Files.createDirectory(directory);
        } catch (IOException error) {
            reason = error.toString();
            close();
            stage = Stage.COMPLETE;
            return;
        }
        host.beginBatch();
        for (CaptureTargetDescriptor target : catalog.targets()) {
            if (!target.available()) result(target, target.availability(), target.reason(), null);
            else if (!targetIds.contains(target.id())) result(target, "not_selected", "", null);
        }
        for (CaptureTargetDescriptor target : selected) {
            if (!target.available() || navigators.containsKey(target.providerId())) continue;
            try {
                navigators.put(
                    target.providerId(),
                    registry.batchProvider(target.providerId())
                        .navigator(catalog.context(), catalog.options()));
            } catch (RuntimeException error) {
                for (CaptureTargetDescriptor sameProvider : selected) {
                    if (sameProvider.providerId()
                        .equals(target.providerId())) result(sameProvider, "failed", error.toString(), null);
                }
            }
        }
        stage = Stage.SELECTING;
    }

    @Override
    public void cancel() {
        if (!busy()) return;
        cancelRequested = true;
        host.cancelCapture();
    }

    @Override
    public boolean busy() {
        return stage != Stage.IDLE && stage != Stage.COMPLETE;
    }

    public boolean needsFrame() {
        return stage == Stage.WAITING;
    }

    @Override
    public CaptureBatchProgress progress() {
        return new CaptureBatchProgress(
            stage.name()
                .toLowerCase(Locale.ROOT),
            nextTarget,
            selected.size(),
            current == null ? "" : current.label(),
            reason);
    }

    @Override
    public List<CaptureBatchResult> results() {
        return List.copyOf(results.values());
    }

    @Override
    public boolean recordingActive() {
        return host.recordingActive();
    }

    @Override
    public boolean recordingPaused() {
        return host.recordingPaused();
    }

    @Override
    public void pauseCapture() {
        host.pauseCapture();
    }

    @Override
    public void stopCapture() {
        host.stopCapture();
    }

    public void tick(long now) {
        if (!busy()) return;
        switch (stage) {
            case SELECTING -> selectNext(now);
            case WAITING -> {
                host.adoptScreen();
                if (cancelRequested) {
                    result(current, "cancelled", "Batch cancelled", null);
                    stage = Stage.SELECTING;
                } else if (now >= deadline) {
                    result(current, "timeout", reason.isEmpty() ? "GUI readiness timed out" : reason, null);
                    stage = Stage.SELECTING;
                }
            }
            case SAVING -> {
                if (capture.isDone()) {
                    try {
                        result(current, "success", "", capture.join());
                    } catch (CompletionException error) {
                        result(
                            current,
                            cancelRequested ? "cancelled" : "failed",
                            error.getCause()
                                .toString(),
                            null);
                    } catch (RuntimeException error) {
                        result(current, cancelRequested ? "cancelled" : "failed", error.toString(), null);
                    }
                    capture = null;
                    stage = Stage.SELECTING;
                }
            }
            case RESTORING -> restore();
            case REPORTING -> {
                if (report.isDone()) {
                    try {
                        reason = report.join()
                            .toString();
                    } catch (CompletionException error) {
                        reason = error.getCause()
                            .toString();
                    }
                    stage = Stage.COMPLETE;
                }
            }
            default -> {}
        }
    }

    private void selectNext(long now) {
        while (nextTarget < selected.size()) {
            CaptureTargetDescriptor target = selected.get(nextTarget++);
            if (results.containsKey(target.id())) continue;
            if (cancelRequested || attempted >= catalog.options()
                .maxTargets()) {
                result(
                    target,
                    cancelRequested ? "cancelled" : "not_processed",
                    cancelRequested ? "Batch cancelled" : "The configured target limit was reached",
                    null);
                continue;
            }
            current = target;
            attempted++;
            try {
                navigators.get(target.providerId())
                    .select(target);
                host.adoptScreen();
                deadline = now + catalog.options()
                    .timeoutSeconds() * 1_000_000_000L;
                lastRevision = lastFrame = Long.MIN_VALUE;
                stableFrames = 0;
                reason = "";
                stage = Stage.WAITING;
                return;
            } catch (RuntimeException error) {
                result(target, "failed", error.toString(), null);
            }
        }
        stage = Stage.RESTORING;
    }

    public void frameReady(CaptureBatchContext context) {
        if (stage != Stage.WAITING || cancelRequested
            || context.capture()
                .frameId() == lastFrame)
            return;
        lastFrame = context.capture()
            .frameId();
        try {
            CaptureBatchNavigator navigator = navigators.get(current.providerId());
            CaptureBatchReadiness readiness = navigator.readiness(context);
            reason = readiness.reason();
            if (!readiness.ready()) {
                stableFrames = 0;
                lastRevision = Long.MIN_VALUE;
                return;
            }
            stableFrames = readiness.revision() == lastRevision ? stableFrames + 1 : 1;
            lastRevision = readiness.revision();
            if (stableFrames < catalog.options()
                .stableFrames()) return;
            List<CaptureRect> rectangles = navigator.regions(context);
            if (rectangles.isEmpty()) {
                result(current, "unavailable", "The target has no visible capture area", null);
                stage = Stage.SELECTING;
                return;
            }
            CaptureBatchContext source = current.scope()
                .equals(CaptureBatchOptions.VISIBLE) ? catalog.context() : context;
            List<CaptureRegion> regions = new ArrayList<>();
            for (int index = 0; index < rectangles.size(); index++)
                regions.add(new CaptureRegion(index + 1, rectangles.get(index), current.id(), false));
            CaptureOptions settings = catalog.options()
                .output();
            Path output = directory.resolve(
                current.providerId()
                    .replaceAll("[^a-zA-Z0-9_-]", "_"));
            CaptureExportRequest request = new CaptureExportRequest(
                id,
                current.label(),
                source.capture()
                    .viewport(),
                regions,
                new CaptureOptions(
                    settings.resolution(),
                    settings.content(),
                    settings.layout(),
                    output,
                    settings.temporaryBudgetBytes()));
            CaptureFrame frame = current.scope()
                .equals(CaptureBatchOptions.VISIBLE) ? visibleFrame : host.frame();
            capture = host.capture(request, catalog.options(), frame);
            stage = Stage.SAVING;
        } catch (RuntimeException error) {
            result(current, "failed", error.toString(), null);
            stage = Stage.SELECTING;
        }
    }

    private void result(CaptureTargetDescriptor target, String status, String detail, CaptureOutputArtifact artifact) {
        results.put(
            target.id(),
            new CaptureBatchResult(
                target.id(),
                target.providerId(),
                target.scope(),
                target.label(),
                status,
                artifact == null ? ""
                    : directory.relativize(artifact.path())
                        .toString(),
                artifact == null ? 0 : artifact.width(),
                artifact == null ? 0 : artifact.height(),
                artifact == null ? 0 : artifact.frameCount(),
                detail));
    }

    private void restore() {
        List<CaptureBatchNavigator> reverse = new ArrayList<>(navigators.values());
        if (catalog.options()
            .restoreOriginal()) {
            restoration = "success";
            for (int index = reverse.size() - 1; index >= 0; index--) {
                try {
                    reverse.get(index)
                        .restore();
                } catch (RuntimeException error) {
                    restoration = "failed";
                    restorationReason += error.toString() + "\n";
                }
            }
        }
        for (CaptureBatchNavigator navigator : reverse) {
            try {
                navigator.close();
            } catch (RuntimeException error) {
                restoration = "failed";
                restorationReason += error.toString() + "\n";
            }
        }
        navigators.clear();
        host.adoptScreen();
        host.endBatch();
        if (visibleFrame != null) visibleFrame.close();
        visibleFrame = null;
        List<CaptureBatchResult> ordered = catalog.targets()
            .stream()
            .map(target -> results.get(target.id()))
            .toList();
        report = host.writeManifest(
            new CaptureBatchManifest(
                id,
                catalog.options()
                    .maxTargets(),
                restoration,
                restorationReason,
                ordered),
            directory);
        stage = Stage.REPORTING;
    }

    @Override
    public void close() {
        for (CaptureBatchNavigator navigator : navigators.values()) navigator.close();
        navigators.clear();
        if (visibleFrame != null) visibleFrame.close();
        visibleFrame = null;
    }
}
