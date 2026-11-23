package com.hfstudio.guidenh.capture.server.api;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

public interface CaptureServerGuiProvider {

    String id();

    boolean supports(TileEntity tile);

    List<String> variants(TileEntity tile);

    String label(TileEntity tile, String variant);

    String accessReason(CaptureServerContext context, TileEntity tile, String variant);

    void open(CaptureServerContext context, TileEntity tile, String variant);

    boolean owns(CaptureServerContext context, TileEntity tile, Container container, String variant);
}
