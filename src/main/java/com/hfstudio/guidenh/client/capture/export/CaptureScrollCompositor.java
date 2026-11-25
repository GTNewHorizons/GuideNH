package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.export.CaptureScrollManifest.Entry;
import com.hfstudio.guidenh.client.capture.render.CaptureTileStore;

public class CaptureScrollCompositor {

    private CaptureScrollCompositor() {}

    public record Frame(CaptureBatchResult result, CaptureScrollSlice slice) {}

    public record Source(Frame frame, CaptureScrollRaster raster) {}

    public static List<CaptureBatchOutput> compose(CaptureBatchManifest manifest, CaptureBatchOptions options,
        Path directory, Path staging, long remainingBytes) throws IOException {
        Map<String, List<Frame>> groups = new LinkedHashMap<>();
        for (CaptureBatchResult result : manifest.results()) {
            CaptureScrollSlice slice = CaptureScrollSlice.parse(result.data());
            if (slice != null)
                groups.computeIfAbsent(result.providerId() + ":" + slice.group(), ignored -> new ArrayList<>())
                    .add(new Frame(result, slice));
        }
        List<CaptureBatchOutput> outputs = new ArrayList<>();
        for (Map.Entry<String, List<Frame>> group : groups.entrySet()) {
            try {
                CaptureBatchOutput output = image(
                    group.getKey(),
                    group.getValue(),
                    options,
                    directory,
                    staging,
                    remainingBytes);
                outputs.add(output);
                remainingBytes -= output.byteSize();
            } catch (IOException | RuntimeException error) {
                if (Thread.currentThread()
                    .isInterrupted() || error instanceof CancellationException)
                    throw new CancellationException("Scroll composition cancelled");
                outputs.add(
                    new CaptureBatchOutput(
                        "scroll_image",
                        group.getKey(),
                        "failed",
                        "",
                        0,
                        0,
                        group.getValue()
                            .size(),
                        0,
                        error.toString()));
            }
        }
        return List.copyOf(outputs);
    }

    private static CaptureBatchOutput image(String group, List<Frame> frames, CaptureBatchOptions options,
        Path directory, Path staging, long budget) throws IOException {
        List<Source> sources = new ArrayList<>();
        double scaleX = 0;
        double scaleY = 0;
        double width = 0;
        double height = 0;
        long rawBytes = 0;
        for (Frame frame : frames) {
            scaleX = Math.max(
                scaleX,
                frame.slice()
                    .pixelScaleX());
            scaleY = Math.max(
                scaleY,
                frame.slice()
                    .pixelScaleY());
            width = Math.max(
                width,
                frame.slice()
                    .contentWidth());
            height = Math.max(
                height,
                frame.slice()
                    .contentHeight());
            if (!frame.result()
                .status()
                .equals("success") || frame.result()
                    .file()
                    .isEmpty())
                continue;
            CaptureScrollRaster raster = CaptureScrollRaster.resolve(frame.result(), frame.slice(), directory);
            rawBytes = Math.addExact(
                rawBytes,
                Math.multiplyExact(
                    (long) raster.crop()
                        .width()
                        * (long) raster.crop()
                            .height(),
                    4));
            sources.add(new Source(frame, raster));
        }
        if (sources.isEmpty()) throw new IOException("No scroll frames were captured for this page");
        CaptureOutputSize size = new CaptureOutputSize(
            Math.toIntExact(Math.round(width * scaleX)),
            Math.toIntExact(Math.round(height * scaleY)));
        long encodingBudget = budget - rawBytes;
        if (encodingBudget <= 45) throw new IOException(
            "Scroll row storage exceeds the remaining disk budget; reduce resolution or capture fewer pages");
        Path target = CaptureOutputPaths.next(staging, "scroll_" + group, "scroll");
        Path temporary = Files.createTempDirectory(staging, ".guidenh_scroll_");
        List<CaptureTileStore> decoded = new ArrayList<>();
        List<CaptureScrollRows.Placement> placements = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        long decodedBytes = 0;
        try {
            for (Source source : sources) {
                if (Thread.currentThread()
                    .isInterrupted()) throw new CancellationException("Scroll composition cancelled");
                CaptureTileStore rows = source.raster()
                    .decode(temporary, budget - decodedBytes, options.memoryBudgetBytes());
                decoded.add(rows);
                decodedBytes += rows.size()
                    .rgbaBytes();
                CaptureRect logical = source.frame()
                    .slice()
                    .destination();
                int left = Math.toIntExact(Math.round(logical.x() * scaleX));
                int top = Math.toIntExact(Math.round(logical.y() * scaleY));
                int right = Math.toIntExact(Math.round(logical.right() * scaleX));
                int bottom = Math.toIntExact(Math.round(logical.bottom() * scaleY));
                CaptureRect destination = new CaptureRect(left, top, right - left, bottom - top);
                if (destination.isEmpty()) throw new IOException("The requested resolution makes a scroll slice empty");
                placements.add(new CaptureScrollRows.Placement(rows, destination));
                entries.add(
                    new Entry(
                        source.frame()
                            .result()
                            .targetId(),
                        directory.relativize(
                            source.raster()
                                .file())
                            .toString(),
                        source.raster()
                            .crop(),
                        destination));
            }
            CaptureOutputArtifact artifact;
            long covered;
            try (CaptureScrollRows rows = new CaptureScrollRows(
                size,
                options.output()
                    .content()
                    .backgroundArgb(),
                placements,
                options.memoryBudgetBytes())) {
                covered = rows.coveredPixels();
                artifact = new PngCaptureExporter().exportRows(rows, temporary.resolve("scroll.png"), encodingBudget);
            }
            decoded.clear();
            boolean complete = covered == (long) size.width() * size.height();
            Path metadata = temporary.resolve("scroll.json");
            try (Writer writer = new OutputStreamWriter(
                new CaptureBudgetOutputStream(Files.newOutputStream(metadata), encodingBudget - artifact.byteSize()),
                StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(
                        new CaptureScrollManifest(
                            group,
                            size.width(),
                            size.height(),
                            scaleX,
                            scaleY,
                            complete,
                            covered,
                            entries),
                        writer);
            }
            long bytes = artifact.byteSize() + Files.size(metadata);
            if (Thread.currentThread()
                .isInterrupted()) throw new CancellationException("Scroll composition cancelled");
            Files.move(temporary, target);
            return new CaptureBatchOutput(
                "scroll_image",
                group,
                complete ? "success" : "partial",
                staging.relativize(target)
                    .toString(),
                size.width(),
                size.height(),
                entries.size(),
                bytes,
                complete ? "" : "Some scroll content was not captured; transparent gaps remain in the image");
        } finally {
            try {
                closeSources(decoded);
            } finally {
                CaptureTemporaryFiles.remove(temporary);
            }
        }
    }

    private static void closeSources(List<CaptureTileStore> sources) throws IOException {
        IOException failure = null;
        for (CaptureTileStore source : sources) {
            try {
                source.close();
            } catch (IOException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

}
