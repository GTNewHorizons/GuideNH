package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public interface CaptureBackendControls {

    List<String> sourceIds();

    List<String> readerIds();

    void selectSource(String id);

    void selectReader(String id);

    CaptureBackendSelection activeBackend();
}
