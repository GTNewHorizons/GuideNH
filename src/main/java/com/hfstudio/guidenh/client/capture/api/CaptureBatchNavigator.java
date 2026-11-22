package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public interface CaptureBatchNavigator extends AutoCloseable {

    void select(CaptureTargetDescriptor target);

    CaptureBatchReadiness readiness(CaptureBatchContext context);

    List<CaptureRect> regions(CaptureBatchContext context);

    void restore();

    @Override
    default void close() {}
}
