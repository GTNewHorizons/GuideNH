package com.hfstudio.guidenh.client.capture.api;

import java.io.IOException;

public interface CaptureRows extends AutoCloseable {

    CaptureOutputSize size();

    void row(int y, int[] target) throws IOException;

    @Override
    default void close() throws IOException {}
}
