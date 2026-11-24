package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationExporter;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureBatchPageManifest.Entry;

public class CaptureBatchPageCompositor {

    private CaptureBatchPageCompositor() {}

    public static List<CaptureBatchOutput> compose(CaptureBatchManifest manifest, CaptureBatchOptions options,
        Path directory, Path staging, long remainingBytes, CaptureAnimationExporter encoder) throws IOException {
        Map<String, List<CaptureContactSheetEntry>> groups = new LinkedHashMap<>();
        List<CaptureBatchOutput> outputs = new ArrayList<>();
        for (CaptureBatchResult result : manifest.results()) {
            if (!result.status()
                .equals("success") || result.file()
                    .isEmpty())
                continue;
            String group = CaptureBatchOutputEntries.group(result, options.layout());
            try {
                groups.computeIfAbsent(group, ignored -> new ArrayList<>())
                    .addAll(CaptureBatchOutputEntries.entries(result, directory));
            } catch (IOException | RuntimeException error) {
                checkCancellation();
                outputs.add(failed(group, result.label() + ": " + error.getMessage()));
            }
        }
        for (Map.Entry<String, List<CaptureContactSheetEntry>> group : groups.entrySet()) {
            checkCancellation();
            if (group.getValue()
                .isEmpty()) continue;
            try {
                CaptureBatchOutput output = group(
                    manifest,
                    group.getKey(),
                    group.getValue(),
                    options,
                    directory,
                    staging,
                    remainingBytes,
                    encoder);
                outputs.add(output);
                remainingBytes -= output.byteSize();
            } catch (IOException | RuntimeException error) {
                checkCancellation();
                outputs.add(failed(group.getKey(), error.toString()));
            }
        }
        return List.copyOf(outputs);
    }

    private static CaptureBatchOutput failed(String group, String reason) {
        return new CaptureBatchOutput("page_animation", group, "failed", "", 0, 0, 0, 0, reason);
    }

    private static CaptureBatchOutput group(CaptureBatchManifest manifest, String group,
        List<CaptureContactSheetEntry> entries, CaptureBatchOptions options, Path directory, Path staging, long budget,
        CaptureAnimationExporter encoder) throws IOException {
        Path target = CaptureOutputPaths.next(staging, "pages_" + group, "pages");
        Path temporary = Files.createTempDirectory(staging, ".guidenh_pages_");
        try {
            List<CaptureOutputSize> pageSizes = new ArrayList<>(entries.size());
            int width = 0;
            int height = 0;
            for (CaptureContactSheetEntry entry : entries) {
                checkCancellation();
                CaptureOutputSize size = CaptureBatchPageReader.size(
                    entry,
                    options.layout()
                        .pageMaximumDimension());
                pageSizes.add(size);
                width = Math.max(width, size.width());
                height = Math.max(height, size.height());
            }
            CaptureOutputSize canvas = new CaptureOutputSize(width, height);
            long frameBytes = (long) width * height * 4;
            long memoryBudget = Math.min(
                options.memoryBudgetBytes(),
                options.layout()
                    .pageRecording()
                    .memoryBudgetBytes());
            if (frameBytes * 4 > memoryBudget)
                throw new IOException("Page images exceed the memory budget; choose a smaller page size");
            CaptureViewport viewport = new CaptureViewport(width, height, width, height);
            CaptureOptions output = options.output();
            CaptureContentOptions content = output.content();
            CaptureExportRequest request = new CaptureExportRequest(
                manifest.id(),
                group,
                viewport,
                List.of(new CaptureRegion(1, viewport.bounds(), "", false)),
                new CaptureOptions(
                    new CaptureResolution(1, 1, 0, 0, true, "compatible"),
                    new CaptureContentOptions(false, false, false, false, false, content.backgroundArgb() & 0xFFFFFF),
                    new CaptureLayoutOptions("original", 1, 0),
                    temporary,
                    budget,
                    output.backend(),
                    encoder.format()));
            CaptureRecordingOptions initial = options.layout()
                .pageRecording();
            CaptureRecordingOptions recording = new CaptureRecordingOptions(
                encoder.format(),
                60,
                0,
                0,
                initial.loopCount(),
                initial.alphaThreshold(),
                initial.matteTransparency(),
                initial.dither(),
                initial.spriteColumns(),
                initial.spritePadding(),
                initial.spritePageSize(),
                1,
                memoryBudget - frameBytes,
                initial.strictQueue(),
                initial.targetChangePolicy());
            Path encoded = encoder.directory() ? Files.createDirectory(temporary.resolve("animation"))
                : temporary.resolve("pages." + encoder.format());
            long interval = Math.multiplyExact(
                options.layout()
                    .pageDelayMillis(),
                1_000_000L);
            long duration = Math.multiplyExact(entries.size(), interval);
            List<Entry> pages = new ArrayList<>(entries.size());
            CaptureOutputArtifact artifact;
            try (CaptureAnimationWriter writer = encoder.open(request, recording, encoded)) {
                for (int index = 0; index < entries.size(); index++) {
                    checkCancellation();
                    CaptureContactSheetEntry entry = entries.get(index);
                    long elapsed = Math.multiplyExact(index, interval);
                    CaptureOutputSize page = pageSizes.get(index);
                    try (CaptureRecordedFrame frame = new CaptureRecordedFrame(
                        CaptureBatchPageReader
                            .frame(entry, page, canvas, content.backgroundArgb(), elapsed, memoryBudget - frameBytes),
                        elapsed)) {
                        checkCancellation();
                        writer.append(frame);
                    }
                    pages.add(
                        new Entry(
                            entry.targetId(),
                            entry.label(),
                            directory.relativize(entry.source())
                                .toString(),
                            entry.sourceBounds(),
                            CaptureBatchPageReader.destination(page, canvas),
                            elapsed,
                            interval));
                }
                artifact = writer.finish(duration);
            }
            if (artifact.width() != width || artifact.height() != height)
                throw new IOException("The page encoder did not preserve the preview dimensions");
            Path metadata = temporary.resolve("pages.json");
            try (Writer writer = new OutputStreamWriter(
                new CaptureBudgetOutputStream(Files.newOutputStream(metadata), budget - artifact.byteSize()),
                StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(
                        new CaptureBatchPageManifest(group, encoder.format(), width, height, duration, pages),
                        writer);
            }
            long bytes = Math.addExact(artifact.byteSize(), Files.size(metadata));
            if (bytes > budget) throw new IOException("Page animation exceeds the remaining batch disk budget");
            checkCancellation();
            Files.move(temporary, target);
            return new CaptureBatchOutput(
                "page_animation",
                group,
                "success",
                staging.relativize(target)
                    .toString(),
                width,
                height,
                entries.size(),
                bytes,
                "");
        } finally {
            if (Files.exists(temporary)) {
                try (var paths = Files.walk(temporary)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder())
                        .toList()) Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void checkCancellation() {
        if (Thread.currentThread()
            .isInterrupted()) throw new CancellationException("Batch page composition cancelled");
    }
}
