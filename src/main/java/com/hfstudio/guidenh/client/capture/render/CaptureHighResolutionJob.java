package com.hfstudio.guidenh.client.capture.render;

import java.io.IOException;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CaptureMaskedRows;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;

public class CaptureHighResolutionJob implements AutoCloseable {

    private final CaptureExportRequest request;
    private final CaptureRenderSnapshot snapshot;
    private final List<CaptureTile> tiles;
    private final long timestamp;
    private CaptureTileStore store;
    private int nextTile;

    public CaptureHighResolutionJob(CaptureExportRequest request, CaptureRenderSnapshot snapshot, int tileSize,
        long timestamp) throws IOException {
        this.snapshot = snapshot;
        this.timestamp = timestamp;
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
        if (outputBudget <= 0)
            throw new IllegalArgumentException("The high-resolution source leaves no disk space for the PNG output");
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
                outputBudget));
        tiles = CaptureTilePlanner.plan(request, composition, size, tileSize);
        store = new CaptureTileStore(
            request.options()
                .outputDirectory(),
            size,
            request.options()
                .content()
                .backgroundArgb(),
            request.options()
                .temporaryBudgetBytes());
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

    public boolean step(GuiTileCaptureSource source, GlCapturePixelReader reader) throws IOException {
        CaptureTile tile = tiles.get(nextTile);
        try (CaptureFrame frame = source.render(
            tile,
            snapshot,
            reader,
            request.options()
                .content()
                .includeDebug(),
            timestamp)) {
            store.put(tile, frame, GuiTileCaptureSource.GUARD);
        }
        nextTile++;
        return nextTile == tiles.size();
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
            if (store != null) store.close();
        } finally {
            store = null;
            snapshot.close();
        }
    }
}
