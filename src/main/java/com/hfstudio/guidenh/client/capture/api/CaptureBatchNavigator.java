package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public interface CaptureBatchNavigator extends AutoCloseable {

    void select(CaptureTargetDescriptor target);

    CaptureBatchReadiness readiness(CaptureBatchContext context);

    List<CaptureRect> regions(CaptureBatchContext context);

    default List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
        CaptureBatchContext context) {
        return List.of();
    }

    void restore();

    CaptureBatchReadiness restorationReadiness(CaptureBatchContext context);

    @Override
    default void close() {}
}
