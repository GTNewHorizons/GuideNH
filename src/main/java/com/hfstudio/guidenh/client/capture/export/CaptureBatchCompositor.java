package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchManifest;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOutput;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.export.CaptureContactSheetManifest.Entry;
import com.hfstudio.guidenh.client.capture.export.CaptureContactSheetManifest.Page;

public class CaptureBatchCompositor {

    private CaptureBatchCompositor() {}

    public static List<CaptureBatchOutput> compose(CaptureBatchManifest manifest, CaptureBatchOptions options,
        Path directory, long remainingBytes) throws IOException {
        if (!options.layout()
            .contactSheet()) return List.of();
        Map<String, List<CaptureContactSheetEntry>> groups = new LinkedHashMap<>();
        List<CaptureBatchOutput> outputs = new ArrayList<>();
        for (CaptureBatchResult result : manifest.results()) {
            if (!result.status()
                .equals("success") || result.file()
                    .isEmpty())
                continue;
            String group = options.layout()
                .groupByProvider() ? result.providerId() : "all";
            try {
                groups.computeIfAbsent(group, ignored -> new ArrayList<>())
                    .addAll(entries(result, directory));
            } catch (IOException | RuntimeException error) {
                outputs.add(
                    new CaptureBatchOutput(
                        "contact_sheet",
                        group,
                        "failed",
                        "",
                        0,
                        0,
                        1,
                        0,
                        result.label() + ": " + error));
            }
        }
        if (groups.isEmpty()) return List.copyOf(outputs);
        Path staging = Files.createTempDirectory(directory, ".guidenh_contact_batch_");
        Path published = CaptureOutputPaths.next(directory, "contact_sheets", "contact");
        try {
            for (Map.Entry<String, List<CaptureContactSheetEntry>> group : groups.entrySet()) {
                if (group.getValue()
                    .isEmpty()) continue;
                try {
                    CaptureBatchOutput output = sheet(
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
                        throw new CancellationException("Batch composition cancelled");
                    outputs.add(
                        new CaptureBatchOutput(
                            "contact_sheet",
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
            if (Thread.currentThread()
                .isInterrupted()) throw new CancellationException("Batch composition cancelled");
            if (outputs.stream()
                .anyMatch(
                    output -> output.status()
                        .equals("success"))) {
                Files.move(staging, published);
                outputs = outputs.stream()
                    .map(
                        output -> output.status()
                            .equals("success")
                                ? new CaptureBatchOutput(
                                    output.kind(),
                                    output.group(),
                                    output.status(),
                                    directory.relativize(published.resolve(output.file()))
                                        .toString(),
                                    output.width(),
                                    output.height(),
                                    output.entries(),
                                    output.byteSize(),
                                    output.reason())
                                : output)
                    .toList();
            }
            return List.copyOf(outputs);
        } finally {
            removeTemporary(staging);
        }
    }

    private static List<CaptureContactSheetEntry> entries(CaptureBatchResult result, Path directory)
        throws IOException {
        Path source = directory.resolve(result.file());
        if (!Files.isDirectory(source))
            return List.of(new CaptureContactSheetEntry(result.targetId(), result.label(), source));
        Path regions = source.resolve("regions.json");
        if (Files.exists(regions)) {
            List<CaptureContactSheetEntry> entries = new ArrayList<>();
            try (Reader reader = Files.newBufferedReader(regions, StandardCharsets.UTF_8)) {
                for (JsonElement element : new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("regions")) {
                    String file = element.getAsJsonObject()
                        .get("file")
                        .getAsString();
                    String id = element.getAsJsonObject()
                        .get("id")
                        .getAsString();
                    entries.add(
                        new CaptureContactSheetEntry(
                            result.targetId() + "/" + id,
                            result.label() + " #" + id,
                            source.resolve(file)));
                }
            }
            return List.copyOf(entries);
        }
        Path poster = source.resolve("frame_000000.png");
        if (!Files.exists(poster)) poster = source.resolve("sprites_0000.png");
        return List.of(new CaptureContactSheetEntry(result.targetId(), result.label(), poster));
    }

    private static CaptureBatchOutput sheet(String group, List<CaptureContactSheetEntry> source,
        CaptureBatchOptions options, Path directory, Path staging, long budget) throws IOException {
        CaptureBatchLayoutOptions layout = options.layout();
        int columns = Math.min(layout.columns(), source.size());
        int cellWidth = layout.thumbnailSize() + CaptureContactSheetRows.PADDING * 2;
        int cellHeight = cellWidth + CaptureContactSheetRows.CAPTION;
        if ((long) columns * cellWidth > layout.pageSize() || cellHeight > layout.pageSize())
            throw new IOException("Contact sheet cells exceed the page size; reduce columns or thumbnail size");
        int perPage = Math.multiplyExact(columns, layout.pageSize() / cellHeight);
        Path target = CaptureOutputPaths.next(staging, "contact_" + group, "contact");
        Path temporary = Files.createTempDirectory(staging, ".guidenh_contact_");
        long bytes = 0;
        List<Entry> entries = new ArrayList<>(source.size());
        List<Page> pages = new ArrayList<>();
        try {
            for (int first = 0, page = 0; first < source.size(); first += perPage, page++) {
                String file = String.format("page_%04d.png", page);
                List<CaptureContactSheetEntry> current = source
                    .subList(first, Math.min(source.size(), first + perPage));
                CaptureOptions output = options.output();
                CaptureOutputArtifact artifact;
                try (CaptureContactSheetRows rows = new CaptureContactSheetRows(
                    current,
                    columns,
                    layout.thumbnailSize(),
                    output.content()
                        .backgroundArgb(),
                    options.memoryBudgetBytes())) {
                    artifact = new PngCaptureExporter().exportRows(rows, temporary.resolve(file), budget - bytes);
                }
                bytes += artifact.byteSize();
                pages.add(new Page(file, artifact.width(), artifact.height()));
                for (int index = 0; index < current.size(); index++) {
                    CaptureContactSheetEntry entry = current.get(index);
                    entries.add(
                        new Entry(
                            entry.targetId(),
                            entry.label(),
                            directory.relativize(entry.source())
                                .toString(),
                            file,
                            index % columns * cellWidth,
                            index / columns * cellHeight,
                            cellWidth,
                            cellHeight));
                }
            }
            Path metadata = temporary.resolve("contact.json");
            try (Writer writer = new OutputStreamWriter(
                new CaptureBudgetOutputStream(Files.newOutputStream(metadata), budget - bytes),
                StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(new CaptureContactSheetManifest(group, pages, entries), writer);
            }
            bytes += Files.size(metadata);
            if (bytes > budget) throw new IOException("Contact sheet output exceeds the remaining batch disk budget");
            if (Thread.currentThread()
                .isInterrupted()) throw new CancellationException("Contact sheet cancelled");
            Files.move(temporary, target);
            return new CaptureBatchOutput(
                "contact_sheet",
                group,
                "success",
                staging.relativize(target)
                    .toString(),
                pages.getFirst()
                    .width(),
                pages.getFirst()
                    .height(),
                entries.size(),
                bytes,
                "");
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
}
