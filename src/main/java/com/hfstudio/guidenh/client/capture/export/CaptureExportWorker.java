package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.CaptureRegistry;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryReservation;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRowExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.recording.CaptureRecording;

public class CaptureExportWorker implements AutoCloseable {

    private final CaptureRegistry registry;
    private final CaptureMemoryAccount memory;
    private final Set<CaptureExportTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "GuideNH GUI export");
        thread.setDaemon(true);
        return thread;
    });

    public CaptureExportWorker() {
        this(new CaptureRegistry());
    }

    public CaptureExportWorker(CaptureRegistry registry) {
        this(registry, null);
    }

    public CaptureExportWorker(CaptureRegistry registry, CaptureMemoryAccount memory) {
        this.registry = Objects.requireNonNull(registry);
        this.memory = memory;
        if (!registry.formats()
            .contains("png")) register(new PngCaptureExporter());
        if (!registry.formats()
            .contains("gif")) registerAnimation(new GifCaptureExporter());
        if (!registry.formats()
            .contains("frames")) registerAnimation(new PngSequenceCaptureExporter("frames"));
        if (!registry.formats()
            .contains("sprites")) registerAnimation(new PngSequenceCaptureExporter("sprites"));
    }

    public void register(CaptureExporter exporter) {
        registry.registerExporter(exporter);
    }

    public int pendingTasks() {
        return tasks.size();
    }

    public void registerAnimation(CaptureAnimationExporter exporter) {
        registry.registerAnimationExporter(exporter);
    }

    public CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory) {
        return writeManifest(manifest, directory, Long.MAX_VALUE);
    }

    public CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory, long remainingBytes) {
        return submitTask(() -> {
            Path temporary = null;
            try (CaptureMemoryReservation workspace = new CaptureMemoryReservation(memory, 65536)) {
                temporary = Files.createTempFile(directory, ".guidenh_manifest_", ".part");
                try (Writer writer = new OutputStreamWriter(
                    new CaptureBudgetOutputStream(Files.newOutputStream(temporary), remainingBytes),
                    StandardCharsets.UTF_8)) {
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

    public CompletableFuture<List<CaptureBatchOutput>> compose(CaptureBatchManifest manifest,
        CaptureBatchOptions options, Path directory, long remainingBytes) {
        CaptureAnimationExporter pageEncoder = options.layout()
            .pageRecording() == null ? null
                : registry.animationExporter(
                    options.layout()
                        .pageRecording()
                        .format());
        if (!options.layout()
            .contactSheet()
            && !options.layout()
                .scrollImages()
            && pageEncoder == null) return CompletableFuture.completedFuture(List.of());
        return submitTask(() -> {
            long available = memory == null ? options.memoryBudgetBytes()
                : Math.min(options.memoryBudgetBytes(), memory.available());
            CaptureBatchOptions bounded = options.withMemoryBudget(available);
            try (CaptureMemoryReservation workspace = new CaptureMemoryReservation(memory, available)) {
                return CaptureBatchCompositor.compose(manifest, bounded, directory, remainingBytes, pageEncoder);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }, () -> {});
    }

    public void startRecording(CaptureRecording recording) {
        CaptureAnimationExporter exporter = registry.animationExporter(
            recording.options()
                .format());
        recording.memoryAccount(memory);
        long bytes = exporter.workspaceBytes(recording.request(), recording.options(), outputSize(recording.request()));
        if (bytes > recording.options()
            .memoryBudgetBytes())
            throw new IllegalArgumentException("The animation workspace exceeds its memory allowance");
        CaptureMemoryReservation workspace = new CaptureMemoryReservation(memory, bytes);
        try {
            recording.completion(submitTask(() -> {
                try (workspace) {
                    try {
                        return exportRecording(recording, exporter);
                    } finally {
                        recording.close();
                    }
                } catch (IOException error) {
                    throw new UncheckedIOException(error);
                } catch (InterruptedException error) {
                    Thread.currentThread()
                        .interrupt();
                    throw new CancellationException("Recording cancelled");
                }
            }, () -> {
                CaptureCleanup cleanup = new CaptureCleanup();
                cleanup.close(recording);
                cleanup.close(workspace);
                cleanup.rethrow();
            }));
        } catch (RuntimeException | Error failure) {
            CaptureCleanup cleanup = new CaptureCleanup(failure);
            cleanup.close(workspace);
            throw failure;
        }
    }

    private static CaptureOutputSize outputSize(CaptureExportRequest request) {
        CaptureComposition composition = CaptureRowComposer.compose(request);
        return request.options()
            .resolution()
            .resolve(
                composition.size()
                    .width(),
                composition.size()
                    .height());
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
            CaptureTemporaryFiles.remove(temporary);
        }
    }

    public CompletableFuture<CaptureOutputArtifact> submitRows(CaptureExportRequest request, CaptureRows rows) {
        return submitRows(request, rows, 0);
    }

    public CompletableFuture<CaptureOutputArtifact> submitRows(CaptureExportRequest request, CaptureRows rows,
        long timestampNanos) {
        CaptureExporter encoder = registry.exporter(
            request.options()
                .format());
        if (!(encoder instanceof CaptureRowExporter rowEncoder)) throw new IllegalArgumentException(
            "This capture format does not support row streaming: " + encoder.format());
        return submitRows(request, rows, timestampNanos, rowEncoder);
    }

    public CompletableFuture<CaptureOutputArtifact> submitRows(CaptureExportRequest request, CaptureRows rows,
        long timestampNanos, CaptureRowExporter encoder) {
        Objects.requireNonNull(encoder);
        return submitTask(() -> {
            Path temporary = null;
            try (rows;
                CaptureMemoryReservation workspace = new CaptureMemoryReservation(
                    memory,
                    encoder.workspaceBytes(request, rows.size()))) {
                Path directory = request.options()
                    .outputDirectory();
                boolean separate = request.options()
                    .layout()
                    .separateRegions();
                Path target = CaptureOutputPaths
                    .next(directory, request.sourceId(), separate ? "regions" : encoder.format());
                temporary = separate ? Files.createTempDirectory(directory, ".guidenh_regions_")
                    : Files.createTempFile(directory, ".guidenh_capture_", ".part");
                CaptureOutputArtifact artifact = separate
                    ? CaptureRegionExporter
                        .export(request, rows, temporary, timestampNanos, encoder.format(), encoder::exportRows)
                    : encoder.exportRows(request, rows, temporary);
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
        return submit(request, frame, registry.exporter(format));
    }

    public CompletableFuture<CaptureOutputArtifact> submit(CaptureExportRequest request, CaptureFrame frame,
        CaptureExporter exporter) {
        Objects.requireNonNull(exporter);
        CaptureFrame owned = frame.account(memory)
            .retain();
        return submitTask(() -> {
            try (owned;
                CaptureMemoryReservation workspace = new CaptureMemoryReservation(
                    memory,
                    exporter.workspaceBytes(request, outputSize(request)))) {
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
        boolean separate = request.options()
            .layout()
            .separateRegions();
        Path target = CaptureOutputPaths.next(directory, request.sourceId(), separate ? "regions" : exporter.format());
        Path temporary = separate ? Files.createTempDirectory(directory, ".guidenh_regions_")
            : Files.createTempFile(directory, ".guidenh_capture_", ".part");
        try {
            CaptureOutputArtifact artifact;
            if (!separate) artifact = exporter.export(request, frame, temporary);
            else if (exporter instanceof CaptureRowExporter rowEncoder) {
                try (CaptureRows rows = new CaptureRowComposer(request, frame)) {
                    artifact = CaptureRegionExporter.export(
                        request,
                        rows,
                        temporary,
                        frame.timestampNanos(),
                        exporter.format(),
                        rowEncoder::exportRows);
                }
            } else {
                try (CaptureRows rows = new CaptureRowComposer(request, frame)) {
                    artifact = CaptureRegionExporter.export(
                        request,
                        rows,
                        temporary,
                        frame.timestampNanos(),
                        exporter.format(),
                        (bounded, selected, targetFile) -> exporter.export(bounded, frame, targetFile));
                }
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
            removeTemporary(temporary);
        }
    }

    private static void removeTemporary(Path temporary) throws IOException {
        CaptureTemporaryFiles.remove(temporary);
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
