package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
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

    public void registerAnimation(CaptureAnimationExporter exporter) {
        animationExporters.put(exporter.format(), exporter);
    }

    public void startRecording(CaptureRecording recording) {
        CaptureAnimationExporter exporter = animationExporters.get(
            recording.options()
                .format());
        if (exporter == null) throw new IllegalArgumentException(
            "Unknown animation format: " + recording.options()
                .format());
        recording.completion(CompletableFuture.supplyAsync(() -> {
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
        }, executor));
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
            Files.move(temporary, target);
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
        return CompletableFuture.supplyAsync(() -> {
            Path temporary = null;
            try (rows) {
                Path directory = request.options()
                    .outputDirectory();
                Path target = CaptureOutputPaths.next(directory, request.sourceId(), "png");
                temporary = Files.createTempFile(directory, ".guidenh_capture_", ".part");
                CaptureOutputArtifact artifact = new PngCaptureExporter().exportRows(request, rows, temporary);
                Files.move(temporary, target);
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
                        Files.deleteIfExists(temporary);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                }
            }
        }, executor);
    }

    public CompletableFuture<CaptureOutputArtifact> submit(CaptureExportRequest request, CaptureFrame frame,
        String format) {
        CaptureExporter exporter = exporters.get(format);
        if (exporter == null) throw new IllegalArgumentException("Unknown capture format: " + format);
        CaptureFrame owned = frame.retain();
        return CompletableFuture.supplyAsync(() -> {
            try (owned) {
                return export(request, owned, exporter);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }, executor);
    }

    private CaptureOutputArtifact export(CaptureExportRequest request, CaptureFrame frame, CaptureExporter exporter)
        throws IOException {
        Path directory = request.options()
            .outputDirectory();
        Path target = CaptureOutputPaths.next(directory, request.sourceId(), exporter.format());
        Path temporary = Files.createTempFile(directory, ".guidenh_capture_", ".part");
        try {
            CaptureOutputArtifact artifact = exporter.export(request, frame, temporary);
            Files.move(temporary, target);
            return new CaptureOutputArtifact(
                target,
                artifact.format(),
                artifact.width(),
                artifact.height(),
                artifact.frameCount(),
                artifact.byteSize());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
