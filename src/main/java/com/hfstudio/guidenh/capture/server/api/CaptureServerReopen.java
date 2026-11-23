package com.hfstudio.guidenh.capture.server.api;

import net.minecraft.tileentity.TileEntity;

public interface CaptureServerReopen {

    TileEntity source();

    String accessReason(CaptureServerContext context);

    void open(CaptureServerContext context);
}
