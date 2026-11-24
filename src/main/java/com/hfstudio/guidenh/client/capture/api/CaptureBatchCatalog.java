package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public record CaptureBatchCatalog(CaptureBatchContext context, CaptureBatchOptions options,
    List<CaptureTargetDescriptor> targets) {

    public CaptureBatchCatalog {
        targets = List.copyOf(targets);
    }
}
