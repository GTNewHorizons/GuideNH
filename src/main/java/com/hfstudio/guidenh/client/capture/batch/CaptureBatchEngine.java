package com.hfstudio.guidenh.client.capture.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    public record Stage(String id) {

        public static final Stage IDLE = new Stage("idle");
        public static final Stage SELECTING = new Stage("selecting");
        public static final Stage WAITING = new Stage("waiting");
        public static final Stage SAVING = new Stage("saving");
        public static final Stage RESTORING = new Stage("restoring");
        public static final Stage RESTORE_WAIT = new Stage("restore_wait");
        public static final Stage REPORTING = new Stage("reporting");
        public static final Stage COMPLETE = new Stage("complete");
    }

    private final CaptureRegistry registry;
    private final CaptureBatchHost host;
    private final Map<String, CaptureBatchNavigator> navigators = new LinkedHashMap<>();
    private final Set<String> expanded = new HashSet<>();
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
        expanded.clear();
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
        return stage == Stage.WAITING || stage == Stage.RESTORE_WAIT;
    }

    @Override
    public CaptureBatchProgress progress() {
        return new CaptureBatchProgress(
            stage.id(),
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
        switch (stage.id()) {
            case "selecting" -> selectNext(now);
            case "waiting" -> {
                host.adoptScreen();
                if (cancelRequested) {
                    result(current, "cancelled", "Batch cancelled", null);
                    stage = Stage.SELECTING;
                } else if (now >= deadline) {
                    result(current, "timeout", reason.isEmpty() ? "GUI readiness timed out" : reason, null);
                    stage = Stage.SELECTING;
                }
            }
            case "saving" -> {
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
            case "restoring" -> restore(now);
            case "restore_wait" -> {
                host.adoptScreen();
                if (now >= deadline) {
                    restoration = "failed";
                    restorationReason += "Navigation restoration timed out\n";
                    finishRestoration();
                }
            }
            case "reporting" -> {
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
        if (stage == Stage.RESTORE_WAIT) {
            boolean ready = true;
            for (CaptureBatchNavigator navigator : navigators.values()) {
                try {
                    CaptureBatchReadiness state = navigator.restorationReadiness(context);
                    ready &= state.ready();
                    if (!state.ready()) reason = state.reason();
                } catch (RuntimeException error) {
                    restoration = "failed";
                    restorationReason += error.toString() + "\n";
                }
            }
            if (ready) finishRestoration();
            return;
        }
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
            if (expanded.add(current.id())) {
                List<CaptureTargetDescriptor> additional = navigator.additionalTargets(current, context);
                if (!additional.isEmpty()) {
                    List<CaptureTargetDescriptor> all = new ArrayList<>(catalog.targets());
                    List<CaptureTargetDescriptor> pending = new ArrayList<>(selected);
                    Set<String> known = new HashSet<>();
                    for (CaptureTargetDescriptor target : all) known.add(target.id());
                    for (CaptureTargetDescriptor target : additional) {
                        if (known.add(target.id())) {
                            all.add(target);
                            pending.add(target);
                            if (!target.available()) result(target, target.availability(), target.reason(), null);
                        }
                    }
                    catalog = new CaptureBatchCatalog(catalog.context(), catalog.options(), all);
                    selected = List.copyOf(pending);
                }
            }
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
                detail,
                target.data()));
    }

    private void restore(long now) {
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
        host.adoptScreen();
        if (catalog.options()
            .restoreOriginal() && !restoration.equals("failed")) {
            deadline = now + catalog.options()
                .timeoutSeconds() * 1_000_000_000L;
            stage = Stage.RESTORE_WAIT;
        } else finishRestoration();
    }

    private void finishRestoration() {
        for (CaptureBatchNavigator navigator : navigators.values()) {
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
