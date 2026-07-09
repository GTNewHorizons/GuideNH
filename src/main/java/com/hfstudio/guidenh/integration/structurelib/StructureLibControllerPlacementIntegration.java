package com.hfstudio.guidenh.integration.structurelib;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public interface StructureLibControllerPlacementIntegration {

    @Nullable
    TileEntity placeController(GuidebookLevel level, World world,
        StructureLibBuildService.ResolvedController controller);
}
