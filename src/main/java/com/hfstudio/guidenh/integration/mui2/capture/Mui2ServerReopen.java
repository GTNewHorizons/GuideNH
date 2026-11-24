package com.hfstudio.guidenh.integration.mui2.capture;

import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.UIFactory;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiManager;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerReopen;

public class Mui2ServerReopen<D extends GuiData> implements CaptureServerReopen {

    private final UIFactory<D> factory;
    private final D data;
    private final TileEntity tile;

    public Mui2ServerReopen(UIFactory<D> factory, D data, TileEntity tile) {
        this.factory = factory;
        this.data = data;
        this.tile = tile;
    }

    @Override
    public TileEntity source() {
        return tile;
    }

    @Override
    public String accessReason(CaptureServerContext context) {
        return data.getPlayer() == context.player() && factory.canInteractWith(context.player(), data) ? ""
            : "The original MUI2 factory denied access";
    }

    @Override
    public void open(CaptureServerContext context) {
        GuiManager.open(factory, data, context.player());
    }
}
