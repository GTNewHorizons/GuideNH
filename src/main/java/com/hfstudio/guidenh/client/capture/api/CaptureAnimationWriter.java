package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;

public interface CaptureAnimationWriter extends AutoCloseable {

    void append(CaptureRecordedFrame frame) throws IOException;

    CaptureOutputArtifact finish(long elapsedNanos) throws IOException;

    @Override
    void close() throws IOException;
}
