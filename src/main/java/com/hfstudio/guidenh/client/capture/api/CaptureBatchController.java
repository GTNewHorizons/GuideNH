package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.Set;

public interface CaptureBatchController {

    CaptureBatchCatalog catalog();

    void refresh();

    void start(Set<String> targetIds);

    void cancel();

    boolean busy();

    CaptureBatchProgress progress();

    List<CaptureBatchResult> results();

    List<CaptureBatchOutput> outputs();

    boolean recordingActive();

    boolean recordingPaused();

    void pauseCapture();

    void stopCapture();
}
