package com.hfstudio.guidenh.client.capture.api;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CaptureBatchHost {

    CaptureBatchContext context();

    CaptureBatchOptions options();

    CaptureFrame frame();

    boolean readyForBatch();

    void beginBatch();

    void endBatch(boolean restoreSelections);

    void adoptScreen();

    CompletableFuture<CaptureOutputArtifact> capture(CaptureExportRequest request, CaptureBatchOptions options,
        CaptureFrame frame);

    void cancelCapture();

    boolean recordingActive();

    boolean recordingPaused();

    void pauseCapture();

    void stopCapture();

    CompletableFuture<List<CaptureBatchOutput>> compose(CaptureBatchManifest manifest, CaptureBatchOptions options,
        Path directory, long remainingBytes);

    CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory, long remainingBytes);
}
