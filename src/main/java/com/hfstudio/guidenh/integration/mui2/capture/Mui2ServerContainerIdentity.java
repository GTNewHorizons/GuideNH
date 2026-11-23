package com.hfstudio.guidenh.integration.mui2.capture;

import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.hfstudio.guidenh.capture.server.api.CaptureGuiLocation;

public class Mui2ServerContainerIdentity {

    private Mui2ServerContainerIdentity() {}

    public static TileEntity tile(Container container) {
        return container instanceof ModularContainer modular && modular.getGuiData() instanceof PosGuiData data
            ? data.getTileEntity()
            : null;
    }

    public static CaptureGuiLocation location(Container container) {
        return container instanceof ModularContainer modular && modular.getGuiData() instanceof PosGuiData data
            ? new CaptureGuiLocation(data.getWorld().provider.dimensionId, data.getX(), data.getY(), data.getZ())
            : null;
    }
}
