package com.hfstudio.guidenh.integration.ae2.capture;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;

import appeng.container.AEBaseContainer;
import appeng.core.sync.GuiBridge;
import appeng.tile.crafting.TileMolecularAssembler;
import appeng.tile.misc.TileAdvancedInscriber;
import appeng.tile.misc.TileCellWorkbench;
import appeng.tile.misc.TileCondenser;
import appeng.tile.misc.TileInscriber;
import appeng.tile.misc.TileInterface;
import appeng.tile.misc.TileSecurity;
import appeng.tile.misc.TileVibrationChamber;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;
import appeng.tile.storage.TileIOPort;
import appeng.tile.storage.TileSkyChest;
import appeng.util.Platform;

public class Ae2ServerGuiProvider implements CaptureServerGuiProvider {

    private final Map<Class<? extends TileEntity>, GuiBridge> factories = new LinkedHashMap<>();

    public Ae2ServerGuiProvider() {
        factories.put(TileDrive.class, GuiBridge.GUI_DRIVE);
        factories.put(TileChest.class, GuiBridge.GUI_CHEST);
        factories.put(TileSkyChest.class, GuiBridge.GUI_SKYCHEST);
        factories.put(TileIOPort.class, GuiBridge.GUI_IOPORT);
        factories.put(TileCellWorkbench.class, GuiBridge.GUI_CELL_WORKBENCH);
        factories.put(TileAdvancedInscriber.class, GuiBridge.GUI_ADVANCED_INSCRIBER);
        factories.put(TileInscriber.class, GuiBridge.GUI_INSCRIBER);
        factories.put(TileInterface.class, GuiBridge.GUI_INTERFACE);
        factories.put(TileCondenser.class, GuiBridge.GUI_CONDENSER);
        factories.put(TileSecurity.class, GuiBridge.GUI_SECURITY);
        factories.put(TileVibrationChamber.class, GuiBridge.GUI_VIBRATION_CHAMBER);
        factories.put(TileMolecularAssembler.class, GuiBridge.GUI_MAC);
    }

    @Override
    public String id() {
        return "ae2";
    }

    private GuiBridge factory(TileEntity tile) {
        for (Map.Entry<Class<? extends TileEntity>, GuiBridge> entry : factories.entrySet()) {
            if (entry.getKey()
                .isInstance(tile)) return entry.getValue();
        }
        return null;
    }

    @Override
    public boolean supports(TileEntity tile) {
        return factory(tile) != null;
    }

    @Override
    public List<String> variants(TileEntity tile) {
        return List.of(factory(tile).name());
    }

    @Override
    public String label(TileEntity tile, String variant) {
        return tile.getClass()
            .getSimpleName();
    }

    @Override
    public String accessReason(CaptureServerContext context, TileEntity tile, String variant) {
        return GuiBridge.valueOf(variant)
            .hasPermissions(tile, tile.xCoord, tile.yCoord, tile.zCoord, ForgeDirection.UNKNOWN, context.player()) ? ""
                : "AE2 GUI access was denied";
    }

    @Override
    public void open(CaptureServerContext context, TileEntity tile, String variant) {
        Platform.openGUI(context.player(), tile, ForgeDirection.UNKNOWN, GuiBridge.valueOf(variant));
    }

    @Override
    public boolean owns(CaptureServerContext context, TileEntity tile, Container container, String variant) {
        return container instanceof AEBaseContainer ae && ae.getTileEntity() == tile
            && GuiBridge.getGuiByContainerClass(container.getClass()) == GuiBridge.valueOf(variant);
    }
}
