package com.hfstudio.guidenh.integration.capture;

import java.util.List;

import net.minecraft.block.BlockChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.capture.server.CaptureContainerInventories;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorCaptureEnderInventory;

public class VanillaServerGuiProvider implements CaptureServerGuiProvider {

    @Override
    public String id() {
        return "vanilla";
    }

    @Override
    public boolean supports(TileEntity tile) {
        return tile instanceof TileEntityChest || tile instanceof TileEntityEnderChest
            || tile instanceof TileEntityFurnace
            || tile instanceof TileEntityHopper
            || tile instanceof TileEntityDispenser
            || tile instanceof TileEntityBrewingStand
            || tile instanceof TileEntityBeacon;
    }

    @Override
    public List<String> variants(TileEntity tile) {
        return List.of("main");
    }

    @Override
    public String label(TileEntity tile, String variant) {
        return tile instanceof IInventory inventory ? inventory.getInventoryName() : "container.enderchest";
    }

    private IInventory inventory(CaptureServerContext context, TileEntity tile) {
        if (tile instanceof TileEntityEnderChest) return context.player()
            .getInventoryEnderChest();
        if (tile instanceof TileEntityChest) {
            if (!chestNeighborhoodLoaded(context, tile)) return null;
            return ((BlockChest) context.world()
                .getBlock(tile.xCoord, tile.yCoord, tile.zCoord))
                    .func_149951_m(context.world(), tile.xCoord, tile.yCoord, tile.zCoord);
        }
        return (IInventory) tile;
    }

    @Override
    public String accessReason(CaptureServerContext context, TileEntity tile, String variant) {
        if (tile instanceof TileEntityChest && !chestNeighborhoodLoaded(context, tile))
            return "Adjacent chest chunks are not loaded";
        if (tile instanceof TileEntityEnderChest && context.world()
            .isSideSolid(tile.xCoord, tile.yCoord + 1, tile.zCoord, ForgeDirection.DOWN)) return "The chest is blocked";
        if (inventory(context, tile) == null) return "The chest is blocked";
        return "";
    }

    private boolean chestNeighborhoodLoaded(CaptureServerContext context, TileEntity tile) {
        return context.world()
            .blockExists(tile.xCoord - 1, tile.yCoord, tile.zCoord)
            && context.world()
                .blockExists(tile.xCoord + 1, tile.yCoord, tile.zCoord)
            && context.world()
                .blockExists(tile.xCoord, tile.yCoord, tile.zCoord - 1)
            && context.world()
                .blockExists(tile.xCoord, tile.yCoord, tile.zCoord + 1);
    }

    @Override
    public void open(CaptureServerContext context, TileEntity tile, String variant) {
        if (tile instanceof TileEntityFurnace furnace) context.player()
            .func_146101_a(furnace);
        else if (tile instanceof TileEntityHopper hopper) context.player()
            .func_146093_a(hopper);
        else if (tile instanceof TileEntityDispenser dispenser) context.player()
            .func_146102_a(dispenser);
        else if (tile instanceof TileEntityBrewingStand brewing) context.player()
            .func_146098_a(brewing);
        else if (tile instanceof TileEntityBeacon beacon) context.player()
            .func_146104_a(beacon);
        else {
            if (tile instanceof TileEntityEnderChest chest) context.player()
                .getInventoryEnderChest()
                .func_146031_a(chest);
            context.player()
                .displayGUIChest(inventory(context, tile));
        }
    }

    @Override
    public boolean owns(CaptureServerContext context, TileEntity tile, Container container, String variant) {
        if (tile instanceof TileEntityEnderChest && ((AccessorCaptureEnderInventory) context.player()
            .getInventoryEnderChest()).guidenh$captureChest() != tile) return false;
        IInventory inventory = inventory(context, tile);
        return inventory != null && CaptureContainerInventories.contains(container, inventory);
    }
}
