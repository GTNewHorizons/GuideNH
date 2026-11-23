package com.hfstudio.guidenh.integration.gregtech.capture;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.capture.server.CaptureContainerInventories;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.CommonMetaTileEntity;

public class GregTechServerGuiProvider implements CaptureServerGuiProvider {

    @Override
    public String id() {
        return "gregtech";
    }

    @Override
    public boolean supports(TileEntity tile) {
        return tile instanceof BaseMetaTileEntity;
    }

    @Override
    public List<String> variants(TileEntity tile) {
        return List.of("main");
    }

    @Override
    public String label(TileEntity tile, String variant) {
        return ((BaseMetaTileEntity) tile).getInventoryName();
    }

    @Override
    public String accessReason(CaptureServerContext context, TileEntity tile, String variant) {
        BaseMetaTileEntity base = (BaseMetaTileEntity) tile;
        if (!base.canAccessData() || !(base.getMetaTileEntity() instanceof CommonMetaTileEntity))
            return "This machine has no registered modular GUI factory";
        if (base.privateAccess() && !context.player()
            .getUniqueID()
            .equals(base.getOwnerUuid())) return "The machine is owned by another player";
        if (!base.getMetaTileEntity()
            .isAccessAllowed(context.player())) return "Machine access was denied";
        return "";
    }

    @Override
    public void open(CaptureServerContext context, TileEntity tile, String variant) {
        ((CommonMetaTileEntity) ((BaseMetaTileEntity) tile).getMetaTileEntity()).openGui(context.player());
    }

    @Override
    public boolean owns(CaptureServerContext context, TileEntity tile, Container container, String variant) {
        return CaptureContainerInventories.contains(container, (BaseMetaTileEntity) tile);
    }
}
