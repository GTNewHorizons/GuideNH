package com.hfstudio.guidenh.integration.gregtech;

import java.util.List;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibBuildRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibBuildService;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerCandidate;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerDiscoveryIntegration;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerPlacementIntegration;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewItemProvider;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewStateSynchronizer;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;

public class GregTechStructureLibControllerIntegration implements StructureLibControllerDiscoveryIntegration,
    StructureLibControllerPlacementIntegration, StructureLibPreviewItemProvider, StructureLibPreviewStateSynchronizer {

    public static final String ACTIVE_CONTROLLER_OPTION = StructureLibSceneOptions.GREGTECH_ACTIVE_CONTROLLER_OPTION;
    public static final String PLACE_HATCHES_OPTION = StructureLibSceneOptions.GREGTECH_PLACE_HATCHES_OPTION;

    public GregTechStructureLibControllerIntegration() {}

    @Override
    public void appendCandidates(String blockId, Block block, @Nullable Item item, List<ItemStack> subItems,
        List<StructureLibControllerCandidate> candidates) {
        if (!GregTechHelpers.isMachineItem(item)) return;
        for (ItemStack stack : subItems) {
            if (GregTechHelpers.isMachineStack(stack)) {
                candidates.add(new StructureLibControllerCandidate(blockId, block, stack.getItemDamage(), stack));
            }
        }
    }

    @Override
    @Nullable
    public Object resolveIdentity(StructureLibControllerCandidate candidate, TileEntity controllerTile,
        IConstructable constructable) {
        int metaTileId = GregTechHelpers.resolveMetaTileId(controllerTile, candidate.getMeta());
        return metaTileId > 0 ? new GregTechControllerIdentity(candidate.getBlockId(), metaTileId) : null;
    }

    @Override
    @Nullable
    public TileEntity placeController(GuidebookLevel level, World world,
        StructureLibBuildService.ResolvedController controller) {
        Integer baseMeta = GregTechHelpers.getMachineControllerBaseMeta(controller.block(), controller.meta());
        if (baseMeta == null) return null;

        TileEntity tile = GregTechHelpers
            .createMachineControllerTile(world, controller.block(), controller.meta(), null);
        if (tile == null) return null;

        level.setBlock(
            StructureLibBuildService.CONTROLLER_X,
            StructureLibBuildService.CONTROLLER_Y,
            StructureLibBuildService.CONTROLLER_Z,
            controller.block(),
            baseMeta,
            tile);

        TileEntity placedTile = world.getTileEntity(
            StructureLibBuildService.CONTROLLER_X,
            StructureLibBuildService.CONTROLLER_Y,
            StructureLibBuildService.CONTROLLER_Z);
        if (placedTile == null) return null;

        GregTechHelpers.initializeMachineControllerTile(placedTile, controller.meta(), null);
        if (canUseGregTechPreviewFacing(placedTile)) {
            GregTechHelpers.applyPreviewControllerFacing(placedTile);
        }
        level.setExplicitBlockId(
            StructureLibBuildService.CONTROLLER_X,
            StructureLibBuildService.CONTROLLER_Y,
            StructureLibBuildService.CONTROLLER_Z,
            controller.blockId());
        return placedTile;
    }

    private boolean canUseGregTechPreviewFacing(TileEntity tileEntity) {
        IAlignment alignment = StructureLibBuildService.resolveAlignment(tileEntity);
        if (alignment == null) return true;
        return alignment.getAlignmentLimits()
            .isNewExtendedFacingValid(
                ExtendedFacing.of(GregTechHelpers.defaultPreviewFacing(), Rotation.NORMAL, Flip.NONE));
    }

    @Override
    public void appendPreviewItems(List<ItemStack> stacks) {
        GregTechHelpers.appendMachineStacks(stacks);
    }

    @Override
    public void configureTrigger(ItemStack triggerStack, StructureLibBuildRequest request) {
        boolean placeHatches = Boolean.TRUE.equals(
            request.options()
                .get(PLACE_HATCHES_OPTION));
        boolean forceHatch = Boolean.TRUE.equals(
            request.options()
                .get("structurelib.force_hatch_placement"));
        if (placeHatches || forceHatch) {
            GregTechHelpers.enableHatchPreviewChannel(triggerStack);
        }
    }

    @Override
    public void synchronizePreviewState(TileEntity controllerTile, ItemStack triggerStack,
        StructureLibBuildRequest request) {
        boolean activeController = Boolean.TRUE.equals(
            request.options()
                .get(ACTIVE_CONTROLLER_OPTION));
        GregTechHelpers.synchronizeMultiblockPreviewState(controllerTile, triggerStack, activeController, null);
    }

    public static class GregTechControllerIdentity {

        private final String blockId;
        private final int metaTileId;

        public GregTechControllerIdentity(String blockId, int metaTileId) {
            this.blockId = blockId;
            this.metaTileId = metaTileId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof GregTechControllerIdentity other)) return false;
            return metaTileId == other.metaTileId && Objects.equals(blockId, other.blockId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockId, metaTileId);
        }
    }
}
