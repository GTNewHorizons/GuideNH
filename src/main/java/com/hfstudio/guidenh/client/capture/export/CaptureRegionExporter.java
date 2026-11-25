package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureRegionWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureResolution;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.export.CaptureRegionManifest.Entry;

public class CaptureRegionExporter {

    private CaptureRegionExporter() {}

    public static CaptureOutputArtifact export(CaptureExportRequest request, CaptureRows source, Path directory,
        long timestampNanos) throws IOException {
        PngCaptureExporter encoder = new PngCaptureExporter();
        return export(request, source, directory, timestampNanos, encoder.format(), encoder::exportRows);
    }

    public static CaptureOutputArtifact export(CaptureExportRequest request, CaptureRows source, Path directory,
        long timestampNanos, String format, CaptureRegionWriter writer) throws IOException {
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
            Path target = directory.resolve("region_" + region.id() + "." + format);
            CaptureOptions options = request.options();
            List<CaptureRegion> selectedRegions = new ArrayList<>();
            selectedRegions.add(region);
            request.regions()
                .stream()
                .filter(CaptureRegion::erased)
                .forEach(selectedRegions::add);
            CaptureResolution resolution = new CaptureResolution(
                1,
                1,
                right - left,
                bottom - top,
                false,
                options.resolution()
                    .qualityPolicy(),
                options.resolution()
                    .resampling());
            CaptureExportRequest bounded = new CaptureExportRequest(
                request.taskId(),
                request.sourceId(),
                request.viewport(),
                selectedRegions,
                new CaptureOptions(
                    resolution,
                    options.content(),
                    new CaptureLayoutOptions("original", 1, 0),
                    directory,
                    options.temporaryBudgetBytes() - bytes,
                    options.backend(),
                    format));
            CaptureOutputArtifact artifact = writer
                .export(bounded, new CaptureRegionRows(source, left, top, right - left, bottom - top), target);
            if (artifact.width() != right - left || artifact.height() != bottom - top)
                throw new IOException("The capture exporter did not preserve the requested region dimensions");
            bytes = Math.addExact(bytes, artifact.byteSize());
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
        try (Writer metadataWriter = new OutputStreamWriter(
            new CaptureBudgetOutputStream(
                Files.newOutputStream(metadata),
                request.options()
                    .temporaryBudgetBytes() - bytes),
            StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(
                    new CaptureRegionManifest(
                        request.taskId(),
                        request.sourceId(),
                        timestampNanos,
                        request.regions(),
                        entries),
                    metadataWriter);
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
