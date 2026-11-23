package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.export.CaptureRegionManifest.Entry;

public class CaptureRegionExporter {

    private CaptureRegionExporter() {}

    public static CaptureOutputArtifact export(CaptureExportRequest request, CaptureRows source, Path directory,
        long timestampNanos) throws IOException {
        CaptureComposition composition = CaptureRowComposer.compose(request);
        CaptureOutputSize size = source.size();
        List<CaptureRegion> regions = request.regions()
            .stream()
            .filter(region -> !region.erased())
            .filter(
                region -> !request.viewport()
                    .pixels(region.bounds())
                    .isEmpty())
            .toList();
        List<Entry> entries = new ArrayList<>(regions.size());
        long bytes = 0;
        for (int index = 0; index < regions.size(); index++) {
            CaptureRegion region = regions.get(index);
            CapturePlacement placement = composition.regions()
                .get(index);
            int left = ceiling(
                placement.destinationX(),
                composition.size()
                    .width(),
                size.width());
            int top = ceiling(
                placement.destinationY(),
                composition.size()
                    .height(),
                size.height());
            int right = ceiling(
                placement.destinationX() + placement.width(),
                composition.size()
                    .width(),
                size.width());
            int bottom = ceiling(
                placement.destinationY() + placement.height(),
                composition.size()
                    .height(),
                size.height());
            if (right == left || bottom == top) throw new IOException("The requested size makes a selected area empty");
            Path target = directory.resolve("region_" + region.id() + ".png");
            CaptureOptions options = request.options();
            CaptureExportRequest bounded = new CaptureExportRequest(
                request.taskId(),
                request.sourceId(),
                request.viewport(),
                request.regions(),
                new CaptureOptions(
                    options.resolution(),
                    options.content(),
                    options.layout(),
                    directory,
                    options.temporaryBudgetBytes() - bytes));
            CaptureOutputArtifact artifact = new PngCaptureExporter()
                .exportRows(bounded, new CaptureRegionRows(source, left, top, right - left, bottom - top), target);
            bytes += artifact.byteSize();
            entries.add(
                new Entry(
                    region.id(),
                    region.bounds(),
                    target.getFileName()
                        .toString(),
                    artifact.width(),
                    artifact.height()));
        }
        Path metadata = directory.resolve("regions.json");
        try (Writer writer = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(
                    new CaptureRegionManifest(
                        request.taskId(),
                        request.sourceId(),
                        timestampNanos,
                        request.regions(),
                        entries),
                    writer);
        }
        bytes += Files.size(metadata);
        if (bytes > request.options()
            .temporaryBudgetBytes()) throw new IOException("Region outputs exceed the configured disk budget");
        return new CaptureOutputArtifact(directory, "regions", size.width(), size.height(), 1, bytes);
    }

    private static int ceiling(int coordinate, int base, int output) {
        return (int) (((long) coordinate * output + base - 1) / base);
    }
}
