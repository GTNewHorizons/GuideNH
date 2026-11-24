package com.hfstudio.guidenh.capture.server.api;

import net.minecraft.inventory.Container;

public interface CaptureServerGuiAction {

    // Implementations and their world references stay on the owning server thread.
    CaptureGuiLocation location(CaptureServerContext context);

    String accessReason(CaptureServerContext context);

    void open(CaptureServerContext context);

    boolean owns(CaptureServerContext context, Container container);
}
