package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.recording.CaptureRecording;

public class CaptureExportWorker implements AutoCloseable {

    private final Map<String, CaptureExporter> exporters = new LinkedHashMap<>();
    private final Map<String, CaptureAnimationExporter> animationExporters = new LinkedHashMap<>();
    private final Set<CaptureExportTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "GuideNH GUI export");
        thread.setDaemon(true);
        return thread;
    });

    public CaptureExportWorker() {
        register(new PngCaptureExporter());
        registerAnimation(new GifCaptureExporter());
        registerAnimation(new PngSequenceCaptureExporter("frames"));
        registerAnimation(new PngSequenceCaptureExporter("sprites"));
    }

    public void register(CaptureExporter exporter) {
        exporters.put(exporter.format(), exporter);
    }

    public int pendingTasks() {
        return tasks.size();
    }

    public void registerAnimation(CaptureAnimationExporter exporter) {
        animationExporters.put(exporter.format(), exporter);
    }

    public CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory) {
        return submitTask(() -> {
            Path temporary = null;
            try {
                temporary = Files.createTempFile(directory, ".guidenh_manifest_", ".part");
                try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    new GsonBuilder().setPrettyPrinting()
                        .create()
                        .toJson(manifest, writer);
                }
                Path target = directory.resolve("batch.json");
                publish(temporary, target);
                return target;
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                }
            }
        }, () -> {});
    }

    public void startRecording(CaptureRecording recording) {
        CaptureAnimationExporter exporter = animationExporters.get(
            recording.options()
                .format());
        if (exporter == null) throw new IllegalArgumentException(
            "Unknown animation format: " + recording.options()
                .format());
        recording.completion(submitTask(() -> {
            try {
                return exportRecording(recording, exporter);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            } catch (InterruptedException error) {
                Thread.currentThread()
                    .interrupt();
                throw new CancellationException("Recording cancelled");
            } finally {
                recording.close();
            }
        }, recording::close));
    }

    private CaptureOutputArtifact exportRecording(CaptureRecording recording, CaptureAnimationExporter exporter)
        throws IOException, InterruptedException {
        CaptureExportRequest request = recording.request();
        Path directory = request.options()
            .outputDirectory();
        Path target = CaptureOutputPaths.next(directory, request.sourceId(), exporter.format());
        Path temporary = exporter.directory() ? Files.createTempDirectory(directory, ".guidenh_animation_")
            : Files.createTempFile(directory, ".guidenh_animation_", ".part");
        try {
            CaptureOutputArtifact artifact;
            try (CaptureAnimationWriter writer = exporter.open(request, recording.options(), temporary)) {
                while (!recording.drained()) {
                    if (recording.cancelled()) throw new CancellationException("Recording cancelled");
                    CaptureRecordedFrame frame = recording.poll();
                    if (frame == null) continue;
                    try {
                        writer.append(frame);
                    } finally {
                        recording.consumed(frame);
                    }
                }
                if (recording.cancelled()) throw new CancellationException("Recording cancelled");
                artifact = writer.finish(recording.endTime());
            }
            publish(temporary, target);
            return new CaptureOutputArtifact(
                target,
                artifact.format(),
                artifact.width(),
                artifact.height(),
                artifact.frameCount(),
                artifact.byteSize());
        } finally {
            if (Files.exists(temporary)) {
                try (var paths = Files.walk(temporary)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder())
                        .toList()) Files.deleteIfExists(path);
                }
            }
        }
    }

    public CompletableFuture<CaptureOutputArtifact> submitRows(CaptureExportRequest request, CaptureRows rows) {
        return submitRows(request, rows, 0);
    }

    public CompletableFuture<CaptureOutputArtifact> submitRows(CaptureExportRequest request, CaptureRows rows,
        long timestampNanos) {
        return submitTask(() -> {
            Path temporary = null;
            try (rows) {
                Path directory = request.options()
                    .outputDirectory();
                boolean separate = request.options()
                    .layout()
                    .separateRegions();
                Path target = CaptureOutputPaths.next(directory, request.sourceId(), separate ? "regions" : "png");
                temporary = separate ? Files.createTempDirectory(directory, ".guidenh_regions_")
                    : Files.createTempFile(directory, ".guidenh_capture_", ".part");
                CaptureOutputArtifact artifact = separate
                    ? CaptureRegionExporter.export(request, rows, temporary, timestampNanos)
                    : new PngCaptureExporter().exportRows(request, rows, temporary);
                publish(temporary, target);
                return new CaptureOutputArtifact(
                    target,
                    artifact.format(),
                    artifact.width(),
                    artifact.height(),
                    artifact.frameCount(),
                    artifact.byteSize());
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            } finally {
                if (temporary != null) {
                    try {
                        removeTemporary(temporary);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                }
            }
        }, () -> {
            try {
                rows.close();
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        });
    }

    public CompletableFuture<CaptureOutputArtifact> submit(CaptureExportRequest request, CaptureFrame frame,
        String format) {
        CaptureExporter exporter = exporters.get(format);
        if (exporter == null) throw new IllegalArgumentException("Unknown capture format: " + format);
        CaptureFrame owned = frame.retain();
        return submitTask(() -> {
            try (owned) {
                return export(request, owned, exporter);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }, owned::close);
    }

    private CaptureOutputArtifact export(CaptureExportRequest request, CaptureFrame frame, CaptureExporter exporter)
        throws IOException {
        Path directory = request.options()
            .outputDirectory();
        boolean separate = exporter.format()
            .equals("png")
            && request.options()
                .layout()
                .separateRegions();
        Path target = CaptureOutputPaths.next(directory, request.sourceId(), separate ? "regions" : exporter.format());
        Path temporary = separate ? Files.createTempDirectory(directory, ".guidenh_regions_")
            : Files.createTempFile(directory, ".guidenh_capture_", ".part");
        try {
            CaptureOutputArtifact artifact = separate
                ? CaptureRegionExporter
                    .export(request, new CaptureRowComposer(request, frame), temporary, frame.timestampNanos())
                : exporter.export(request, frame, temporary);
            publish(temporary, target);
            return new CaptureOutputArtifact(
                target,
                artifact.format(),
                artifact.width(),
                artifact.height(),
                artifact.frameCount(),
                artifact.byteSize());
        } finally {
            removeTemporary(temporary);
        }
    }

    private static void removeTemporary(Path temporary) throws IOException {
        if (!Files.exists(temporary)) return;
        try (var paths = Files.walk(temporary)) {
            for (Path path : paths.sorted(Comparator.reverseOrder())
                .toList()) Files.deleteIfExists(path);
        }
    }

    private static void publish(Path temporary, Path target) throws IOException {
        if (Thread.currentThread()
            .isInterrupted()) throw new CancellationException("Capture export cancelled");
        Files.move(temporary, target);
    }

    private <T> CompletableFuture<T> submitTask(Supplier<T> action, Runnable releaseQueued) {
        CaptureExportTask<T> task = new CaptureExportTask<>(action, releaseQueued, tasks::remove);
        tasks.add(task);
        try {
            executor.execute(task);
        } catch (RejectedExecutionException error) {
            task.cancel();
            throw error;
        }
        return task.future();
    }

    @Override
    public void close() {
        executor.shutdown();
        for (CaptureExportTask<?> task : tasks) task.cancel();
        executor.shutdownNow();
    }
}
