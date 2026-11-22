package com.hfstudio.guidenh.client.capture.api;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface CaptureBatchHost {

    CaptureBatchContext context();

    CaptureBatchOptions options();

    CaptureFrame frame();

    boolean readyForBatch();

    void beginBatch();

    void endBatch();

    void adoptScreen();

    CompletableFuture<CaptureOutputArtifact> capture(CaptureExportRequest request, CaptureBatchOptions options,
        CaptureFrame frame);

    void cancelCapture();

    boolean recordingActive();

    boolean recordingPaused();

    void pauseCapture();

    void stopCapture();

    CompletableFuture<Path> writeManifest(CaptureBatchManifest manifest, Path directory);
}
