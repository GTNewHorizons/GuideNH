package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public interface CaptureBatchProvider {

    String id();

    default int navigationOrder() {
        return 0;
    }

    boolean supports(CaptureBatchContext context);

    List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options);

    CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options);
}
