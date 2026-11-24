package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureBatchOutputEntries {

    private CaptureBatchOutputEntries() {}

    public static String group(CaptureBatchResult result, CaptureBatchLayoutOptions layout) {
        if (!layout.groupByProvider()) return "all";
        return result.data()
            .getOrDefault(
                "mod_id",
                result.data()
                    .getOrDefault("provider", result.providerId()));
    }

    public static List<CaptureContactSheetEntry> entries(CaptureBatchResult result, Path directory) throws IOException {
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
        Path animation = source.resolve("animation.json");
        if (Files.exists(animation)) {
            try (Reader reader = Files.newBufferedReader(animation, StandardCharsets.UTF_8)) {
                var frames = new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("frames");
                if (frames == null || frames.size() == 0) throw new IOException("The animation has no poster frame");
                var first = frames.get(0)
                    .getAsJsonObject();
                return List.of(
                    new CaptureContactSheetEntry(
                        result.targetId(),
                        result.label(),
                        source.resolve(
                            first.get("file")
                                .getAsString()),
                        new CaptureRect(
                            first.get("x")
                                .getAsInt(),
                            first.get("y")
                                .getAsInt(),
                            first.get("width")
                                .getAsInt(),
                            first.get("height")
                                .getAsInt())));
            }
        }
        Path poster = source.resolve("frame_000000.png");
        if (!Files.exists(poster)) poster = source.resolve("sprites_0000.png");
        return List.of(new CaptureContactSheetEntry(result.targetId(), result.label(), poster));
    }

}
