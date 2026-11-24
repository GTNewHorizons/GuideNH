package com.hfstudio.guidenh.client.capture.render;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureMaskedRows;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;

public class CaptureHighResolutionJob implements AutoCloseable {

    private final CaptureExportRequest request;
    private final CaptureExportRequest originalRequest;
    private final CaptureRenderSnapshot snapshot;
    private final List<CaptureTile> tiles;
    private final long timestamp;
    private final long readbackBudget;
    private CaptureTileStore store;
    private int nextTile;
    private int submittedTiles;
    private final Deque<TileReadback> pending = new ArrayDeque<>();
    private CaptureFrame reference;
    private final List<CaptureTile> validationTiles = new ArrayList<>();
    private final List<CaptureRect> selectedPixels;
    private final List<CaptureRect> erasedPixels;
    private int nextValidation;
    private CaptureReadback validation;

    public CaptureHighResolutionJob(CaptureExportRequest request, CaptureRenderSnapshot snapshot, int tileSize,
        long timestamp) throws IOException {
        this(
            request,
            snapshot,
            tileSize,
            timestamp,
            Math.multiplyExact(
                Math.multiplyExact(
                    (long) tileSize + GuiTileCaptureSource.GUARD * 2,
                    (long) tileSize + GuiTileCaptureSource.GUARD * 2),
                24));
    }

    public CaptureHighResolutionJob(CaptureExportRequest request, CaptureRenderSnapshot snapshot, int tileSize,
        long timestamp, long memoryBudget) throws IOException {
        this(request, snapshot, tileSize, timestamp, memoryBudget, null);
    }

    public CaptureHighResolutionJob(CaptureExportRequest request, CaptureRenderSnapshot snapshot, int tileSize,
        long timestamp, long memoryBudget, CaptureFrame reference) throws IOException {
        this.snapshot = snapshot;
        this.timestamp = timestamp;
        readbackBudget = memoryBudget;
        originalRequest = request;
        selectedPixels = request.regions()
            .stream()
            .filter(region -> !region.erased())
            .map(
                region -> request.viewport()
                    .pixels(region.bounds()))
            .toList();
        erasedPixels = request.regions()
            .stream()
            .filter(CaptureRegion::erased)
            .map(
                region -> request.viewport()
                    .pixels(region.bounds()))
            .toList();
        CaptureComposition composition = CaptureRowComposer.compose(request);
        CaptureOutputSize size = request.options()
            .resolution()
            .resolve(
                composition.size()
                    .width(),
                composition.size()
                    .height());
        CaptureOptions options = request.options();
        long outputBudget = options.temporaryBudgetBytes() - size.rgbaBytes();
        if (outputBudget <= 0) throw new IllegalArgumentException(
            "The high-resolution source leaves no disk space for the encoded output");
        this.request = new CaptureExportRequest(
            request.taskId(),
            request.sourceId(),
            request.viewport(),
            request.regions(),
            new CaptureOptions(
                options.resolution(),
                options.content(),
                options.layout(),
                options.outputDirectory(),
                outputBudget,
                options.backend(),
                options.format()));
        tiles = CaptureTilePlanner.plan(request, composition, size, tileSize);
        if (reference != null) {
            CaptureViewport viewport = request.viewport();
            int limit = Math.max(1, Math.min(tileSize, 1024));
            for (int y = 0; y < reference.height(); y += limit) {
                for (int x = 0; x < reference.width(); x += limit) {
                    int width = Math.min(limit, reference.width() - x);
                    int height = Math.min(limit, reference.height() - y);
                    CaptureRect pixels = new CaptureRect(x, y, width, height);
                    if (selectedPixels.stream()
                        .noneMatch(
                            area -> !area.intersect(pixels)
                                .isEmpty()))
                        continue;
                    validationTiles.add(
                        new CaptureTile(
                            x,
                            y,
                            width,
                            height,
                            new CaptureRect(
                                x / viewport.scaleX(),
                                y / viewport.scaleY(),
                                width / viewport.scaleX(),
                                height / viewport.scaleY())));
                }
            }
        }
        store = new CaptureTileStore(
            request.options()
                .outputDirectory(),
            size,
            request.options()
                .content()
                .backgroundArgb(),
            request.options()
                .temporaryBudgetBytes());
        this.reference = reference == null ? null : reference.retain();
    }

    public int completedTiles() {
        return nextTile;
    }

    public long timestampNanos() {
        return timestamp;
    }

    public int tileCount() {
        return tiles.size();
    }

    public CaptureContentOptions content() {
        return request.options()
            .content();
    }

    public boolean step(GuiTileCaptureSource source, CapturePixelReader reader) throws IOException {
        return step(source, reader, readbackBudget);
    }

    public boolean step(GuiTileCaptureSource source, CapturePixelReader reader, long memoryBudget) throws IOException {
        if (nextValidation < validationTiles.size()) {
            CaptureTile tile = validationTiles.get(nextValidation);
            if (validation == null) validation = source.enqueue(
                tile,
                snapshot,
                reader,
                request.options()
                    .content()
                    .includeDebug(),
                timestamp,
                memoryBudget);
            if (!validation.ready()) return false;
            CaptureReadback readback = validation;
            validation = null;
            try (readback; CaptureFrame frame = readback.take()) {
                compareSource(tile, frame);
            }
            nextValidation++;
            if (nextValidation < validationTiles.size()) return false;
        }
        while (submittedTiles < tiles.size() && pending.size() < 2) {
            CaptureTile tile = tiles.get(submittedTiles);
            CaptureReadback readback = source.enqueue(
                tile,
                snapshot,
                reader,
                request.options()
                    .content()
                    .includeDebug(),
                timestamp,
                memoryBudget);
            pending.addLast(new TileReadback(tile, readback));
            submittedTiles++;
        }
        while (!pending.isEmpty() && pending.getFirst()
            .readback()
            .ready()) {
            TileReadback next = pending.removeFirst();
            try (CaptureReadback readback = next.readback(); CaptureFrame frame = readback.take()) {
                store.put(next.tile(), frame, GuiTileCaptureSource.GUARD);
            }
            nextTile++;
        }
        return nextTile == tiles.size();
    }

    private void compareSource(CaptureTile tile, CaptureFrame frame) {
        for (int y = 0; y < tile.height(); y++) {
            for (int x = 0; x < tile.width(); x++) {
                int sourceX = tile.x() + x;
                int sourceY = tile.y() + y;
                if (!contains(selectedPixels, sourceX, sourceY) || contains(erasedPixels, sourceX, sourceY)) continue;
                int original = reference.argb(sourceX, sourceY);
                int replayed = frame.argb(x + GuiTileCaptureSource.GUARD, y + GuiTileCaptureSource.GUARD);
                for (int channel = 0; channel < 32; channel += 8) {
                    if (Math.abs((original >>> channel & 255) - (replayed >>> channel & 255)) > 2)
                        throw new CaptureRedrawMismatchException(sourceX, sourceY, original, replayed);
                }
            }
        }
    }

    private static boolean contains(List<CaptureRect> areas, int x, int y) {
        for (CaptureRect area : areas) if (area.contains(x, y)) return true;
        return false;
    }

    public CaptureExportRequest originalRequest() {
        return originalRequest;
    }

    public CaptureFrame reference() {
        if (reference == null) throw new IllegalStateException("The high-resolution source has no native reference");
        return reference.retain();
    }

    public CaptureExportRequest request() {
        return request;
    }

    public CaptureRows takeRows() {
        CaptureRows result = new CaptureMaskedRows(request, store);
        store = null;
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            if (validation != null) validation.close();
            validation = null;
            while (!pending.isEmpty()) pending.removeFirst()
                .readback()
                .close();
            if (store != null) store.close();
        } finally {
            store = null;
            if (reference != null) reference.close();
            reference = null;
            snapshot.close();
        }
    }

    public record TileReadback(CaptureTile tile, CaptureReadback readback) {}
}
