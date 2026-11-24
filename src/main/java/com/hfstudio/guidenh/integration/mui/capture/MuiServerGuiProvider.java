package com.hfstudio.guidenh.integration.mui.capture;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.modularui.api.UIInfos;
import com.gtnewhorizons.modularui.api.screen.ITileWithModularUI;
import com.hfstudio.guidenh.capture.server.CaptureContainerInventories;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;

public class MuiServerGuiProvider implements CaptureServerGuiProvider {

    @Override
    public String id() {
        return "mui1";
    }

    @Override
    public boolean supports(TileEntity tile) {
        return tile instanceof ITileWithModularUI;
    }

    @Override
    public List<String> variants(TileEntity tile) {
        return List.of("main");
    }

    @Override
    public String label(TileEntity tile, String variant) {
        return tile instanceof IInventory inventory ? inventory.getInventoryName()
            : tile.getClass()
                .getSimpleName();
    }

    @Override
    public String accessReason(CaptureServerContext context, TileEntity tile, String variant) {
        return tile instanceof IInventory inventory && !inventory.isUseableByPlayer(context.player())
            && context.player()
                .getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) < 64
                    ? "Tile inventory access was denied"
                    : "";
    }

    @Override
    public void open(CaptureServerContext context, TileEntity tile, String variant) {
        UIInfos.TILE_MODULAR_UI.open(context.player(), context.world(), tile.xCoord, tile.yCoord, tile.zCoord);
    }

    @Override
    public boolean owns(CaptureServerContext context, TileEntity tile, Container container, String variant) {
        return tile instanceof IInventory inventory && CaptureContainerInventories.contains(container, inventory);
    }
}
