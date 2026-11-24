package com.hfstudio.guidenh.integration.mui.capture;

import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.modularui.common.builder.UIInfo;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerReopen;

public class MuiServerReopen implements CaptureServerReopen {

    private final UIInfo<?, ?> factory;
    private final TileEntity tile;

    public MuiServerReopen(UIInfo<?, ?> factory, TileEntity tile) {
        this.factory = factory;
        this.tile = tile;
    }

    @Override
    public TileEntity source() {
        return tile;
    }

    @Override
    public String accessReason(CaptureServerContext context) {
        return "";
    }

    @Override
    public void open(CaptureServerContext context) {
        factory.open(context.player(), context.world(), tile.xCoord, tile.yCoord, tile.zCoord);
    }
}
