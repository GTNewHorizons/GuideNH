package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.export.CaptureAnimationManifest.FrameEntry;

public class PngSequenceCaptureWriter implements CaptureAnimationWriter {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final Path target;
    private final PngCaptureExporter png = new PngCaptureExporter();
    private final List<Long> times = new ArrayList<>();
    private CaptureOutputSize size;
    private long bytes;

    public PngSequenceCaptureWriter(CaptureExportRequest request, CaptureRecordingOptions options, Path target) {
        this.request = request;
        this.options = options;
        this.target = target;
    }

    private Path framePath(int index) {
        return target.resolve(String.format("frame_%06d.png", index));
    }

    @Override
    public void append(CaptureRecordedFrame frame) throws IOException {
        CaptureExportRequest current = frame.request(request);
        CaptureOptions initial = current.options();
        CaptureExportRequest remaining = new CaptureExportRequest(
            current.taskId(),
            current.sourceId(),
            current.viewport(),
            current.regions(),
            new CaptureOptions(
                initial.resolution(),
                initial.content(),
                initial.layout(),
                initial.outputDirectory(),
                remainingBytes(),
                initial.backend(),
                initial.format()));
        CaptureRowComposer rows = new CaptureRowComposer(remaining, frame.frame());
        if (size != null && !size.equals(rows.size()))
            throw new IOException("Animation frames must retain their initial output dimensions");
        CaptureOutputArtifact artifact = png.exportRows(remaining, rows, framePath(times.size()));
        size = rows.size();
        times.add(times.isEmpty() ? 0L : frame.elapsedNanos());
        bytes = Math.addExact(bytes, artifact.byteSize());
        checkBudget();
    }

    private void checkBudget() throws IOException {
        if (bytes > request.options()
            .temporaryBudgetBytes()) throw new IOException("The animation exceeds the configured disk budget");
    }

    private long remainingBytes() {
        return request.options()
            .temporaryBudgetBytes() - bytes;
    }

    @Override
    public CaptureOutputArtifact finish(long elapsedNanos) throws IOException {
        if (times.isEmpty()) throw new IOException("No GUI frames were captured");
        long duration = Math.max(elapsedNanos, times.getLast() + 1_000_000_000L / options.fps());
        List<FrameEntry> entries = options.format()
            .equals("sprites") ? spritePages(duration) : sequenceEntries(duration);
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .create();
        Path metadata = target.resolve("animation.json");
        try (Writer output = new OutputStreamWriter(
            new CaptureBudgetOutputStream(Files.newOutputStream(metadata), remainingBytes()),
            StandardCharsets.UTF_8)) {
            gson.toJson(
                new CaptureAnimationManifest(
                    request.sourceId(),
                    size.width(),
                    size.height(),
                    duration,
                    request.regions(),
                    entries),
                output);
        }
        bytes = Math.addExact(bytes, Files.size(metadata));
        checkBudget();
        return new CaptureOutputArtifact(target, options.format(), size.width(), size.height(), times.size(), bytes);
    }

    private long duration(int index, long endTime) {
        return (index + 1 < times.size() ? times.get(index + 1) : endTime) - times.get(index);
    }

    private List<FrameEntry> sequenceEntries(long endTime) {
        List<FrameEntry> entries = new ArrayList<>(times.size());
        for (int index = 0; index < times.size(); index++) entries.add(
            new FrameEntry(
                framePath(index).getFileName()
                    .toString(),
                0,
                0,
                size.width(),
                size.height(),
                times.get(index),
                duration(index, endTime)));
        return entries;
    }

    private List<FrameEntry> spritePages(long endTime) throws IOException {
        CaptureSpritePageLayout layout = CaptureSpritePageLayout.create(size, options);
        int perPage = layout.framesPerPage();
        List<FrameEntry> entries = new ArrayList<>(times.size());
        for (int first = 0, page = 0; first < times.size(); page++) {
            if (Thread.currentThread()
                .isInterrupted()) throw new IOException("Sprite export cancelled");
            int count = Math.min(perPage, times.size() - first);
            String name = String.format("sprites_%04d.png", page);
            List<Path> sources = new ArrayList<>(count);
            for (int offset = 0; offset < count; offset++) sources.add(framePath(first + offset));
            Path output = target.resolve(name);
            try (CaptureSpritePageRows rows = new CaptureSpritePageRows(layout, size, sources)) {
                CaptureOutputArtifact artifact = png.exportRows(rows, output, remainingBytes());
                bytes = Math.addExact(bytes, artifact.byteSize());
            }
            checkBudget();
            for (int offset = 0; offset < count; offset++) {
                int index = first + offset;
                entries.add(
                    new FrameEntry(
                        name,
                        layout.x(offset),
                        layout.y(offset),
                        size.width(),
                        size.height(),
                        times.get(index),
                        duration(index, endTime)));
                Path source = framePath(first + offset);
                bytes -= Files.size(source);
                Files.delete(source);
            }
            first += count;
        }
        return entries;
    }

    @Override
    public void close() {
        times.clear();
    }
}
