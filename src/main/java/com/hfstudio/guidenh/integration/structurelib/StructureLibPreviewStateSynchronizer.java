package com.hfstudio.guidenh.integration.structurelib;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public interface StructureLibPreviewStateSynchronizer {

    void synchronizePreviewState(TileEntity controllerTile, ItemStack triggerStack,
        StructureLibBuildRequest request);
}
