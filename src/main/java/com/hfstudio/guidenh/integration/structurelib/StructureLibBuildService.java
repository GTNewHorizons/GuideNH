package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import cpw.mods.fml.common.registry.GameRegistry;

public class StructureLibBuildService {

    public static final int CONTROLLER_X = 0;
    public static final int CONTROLLER_Y = 64;
    public static final int CONTROLLER_Z = 0;
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 50;
    public static final int SURVIVAL_BUDGET = Integer.MAX_VALUE;
    public static final int SURVIVAL_MAX_ROUNDS = 256;

    // ========== DTO ==========

    public record ResolvedController(String blockId, Block block, int meta) {}

    // ========== Pipeline ==========

    public StructureLibBuildResult build(StructureLibBuildRequest request) {
        try {
            return doBuild(request);
        } catch (Throwable t) {
            GuideDebugLog.warn("StructureLib build failed for {}", request.controllerId(), t);
            return new StructureLibBuildResult(List.of(), false, t.getMessage());
        }
    }

    private StructureLibBuildResult doBuild(StructureLibBuildRequest request) {
        ResolvedController controller = resolveController(request.controllerId());

        GuidebookLevel level = new GuidebookLevel();
        World world = level.getOrCreateFakeWorld();
        PreviewFakePlayer fakePlayer = new PreviewFakePlayer(world);

        TileEntity controllerTile = placeController(level, world, controller);
        if (controllerTile == null) {
            return new StructureLibBuildResult(
                List.of(),
                false,
                "Failed to place controller: " + request.controllerId());
        }

        StructureLibOrientationHelper.applyDefaultAlignment(controllerTile);
        StructureLibOrientationHelper
            .applyRequestedAlignment(controllerTile, request.facing(), request.rotation(), request.flip());

        ForgeDirection controllerFacing = StructureLibOrientationHelper.resolveControllerFacing(controllerTile);
        fakePlayer.configureForControllerFacing(controllerFacing);

        IConstructable constructable = resolveConstructable(controllerTile);
        if (constructable == null) {
            return new StructureLibBuildResult(
                List.of(),
                false,
                "Controller not constructable: " + request.controllerId());
        }

        ItemStack trigger = createTrigger(request);

        buildStructure(constructable, trigger, fakePlayer, request, controllerTile);
        syncPreviewState(controllerTile, trigger, request);

        return new StructureLibBuildResult(snapshotBlocks(level), true, null);
    }

    // ========== Controller resolution ==========

    public static ResolvedController resolveController(String controllerId) {
        GuideBlockMatcher matcher = GuideBlockMatcher.parse(controllerId);
        Block block = (Block) Block.blockRegistry.getObject(matcher.getBlockId());
        if (block == null || block == Blocks.air) {
            throw new IllegalArgumentException("Could not resolve controller block: " + controllerId);
        }
        return new ResolvedController(matcher.getBlockId(), block, matcher.getMeta() != null ? matcher.getMeta() : 0);
    }

    // ========== Controller placement ==========

    @Nullable
    public static TileEntity placeController(GuidebookLevel level, World world, ResolvedController controller) {
        for (StructureLibControllerPlacementIntegration integration : StructureLibControllerIntegrationRegistry.global()
            .placementIntegrations()) {
            TileEntity tile = integration.placeController(level, world, controller);
            if (tile != null) return tile;
        }

        TileEntity tile = null;
        try {
            if (controller.block()
                .hasTileEntity(controller.meta())) {
                tile = controller.block()
                    .createTileEntity(world, controller.meta());
            }
        } catch (Throwable t) {
            return null;
        }
        if (tile == null) return null;

        level.setBlock(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.block(), controller.meta(), tile);
        TileEntity placed = world.getTileEntity(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        if (placed != null) {
            level.setExplicitBlockId(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.blockId());
        }
        return placed;
    }

    // ========== Constructable resolution ==========

    @Nullable
    public static IConstructable resolveConstructable(TileEntity controllerTile) {
        if (controllerTile instanceof IConstructableProvider provider) {
            IConstructable c = provider.getConstructable();
            if (c != null) return c;
        }
        if (controllerTile instanceof IConstructable c) {
            return c;
        }
        if (IMultiblockInfoContainer.contains(controllerTile.getClass())) {
            IMultiblockInfoContainer<TileEntity> container = IMultiblockInfoContainer.get(controllerTile.getClass());
            if (container != null) {
                IAlignment alignment = StructureLibOrientationHelper.resolveAlignment(controllerTile);
                ExtendedFacing facing = alignment != null ? alignment.getExtendedFacing() : ExtendedFacing.DEFAULT;
                return container.toConstructable(controllerTile, facing);
            }
        }
        return null;
    }

    // ========== Trigger stack ==========

    public static ItemStack createTrigger(StructureLibBuildRequest request) {
        ItemStack stack = new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(MIN_TIER, request.tier()));
        for (Map.Entry<String, Integer> entry : request.channels()
            .entrySet()) {
            Integer value = entry.getValue();
            if (value != null && value > 0) {
                ChannelDataAccessor.setChannelData(stack, entry.getKey(), value);
            }
        }
        for (StructureLibPreviewItemProvider provider : StructureLibControllerIntegrationRegistry.global()
            .previewItemProviders()) {
            provider.configureTrigger(stack, request);
        }
        return stack;
    }

    // ========== Structure construction ==========

    private static void buildStructure(IConstructable constructable, ItemStack trigger, PreviewFakePlayer fakePlayer,
        StructureLibBuildRequest request, TileEntity controllerTile) {
        previewHook(controllerTile, trigger, true);
        boolean useSurvival = constructable instanceof ISurvivalConstructable;
        if (useSurvival) {
            ISurvivalConstructable sc = (ISurvivalConstructable) constructable;
            ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(createItemSource(), fakePlayer);
            int rounds = 0;
            while (rounds++ < SURVIVAL_BUDGET) {
                int result = sc.survivalConstruct(trigger, SURVIVAL_BUDGET, env);
                if (result == -1) {
                    previewHook(controllerTile, trigger, false);
                    return; // success
                }
                if (result == -2) break; // needs creative fallback
                if (result <= 0) break; // no progress
                hatchRefresh(controllerTile, trigger);
            }
        }
        constructable.construct(trigger.copy(), false);
        previewHook(controllerTile, trigger, false);
    }

    private static void previewHook(TileEntity tile, ItemStack trigger, boolean before) {
        try {
            Object mte = tile.getClass()
                .getMethod("getMetaTileEntity")
                .invoke(tile);
            if (mte == null) return;
            String method = before ? "onPreviewConstruct" : "onPreviewStructureComplete";
            mte.getClass()
                .getMethod(method, ItemStack.class)
                .invoke(mte, trigger);
        } catch (Throwable ignored) {}
    }

    private static void hatchRefresh(TileEntity tile, ItemStack trigger) {
        try {
            GregTechHelpers.refreshPreviewHatchList(tile, trigger, null);
        } catch (Throwable ignored) {}
    }

    private static com.gtnewhorizon.structurelib.structure.IItemSource createItemSource() {
        return blockrenderer6343.api.utils.CreativeItemSource.instance;
    }

    // ========== Preview state sync ==========

    private static void syncPreviewState(TileEntity controllerTile, ItemStack trigger,
        StructureLibBuildRequest request) {
        for (StructureLibPreviewStateSynchronizer synchronizer : StructureLibControllerIntegrationRegistry.global()
            .previewStateSynchronizers()) {
            synchronizer.synchronizePreviewState(controllerTile, trigger, request);
        }
    }

    // ========== Block snapshotting ==========

    public static List<StructureLibBuildResult.PlacedBlock> snapshotBlocks(GuidebookLevel level) {
        List<int[]> filledBlocks = new ArrayList<>(level.getFilledBlocks());
        if (filledBlocks.isEmpty()) return List.of();

        // First pass: find min corner (matching old StructureLibRuntimeFacade behavior)
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int[] pos : filledBlocks) {
            if (pos[0] < minX) minX = pos[0];
            if (pos[1] < minY) minY = pos[1];
            if (pos[2] < minZ) minZ = pos[2];
        }
        // Use CONTROLLER position as fallback when min corner is higher (e.g. only controller above ground)
        if (minY > CONTROLLER_Y) minY = CONTROLLER_Y;
        if (minX > CONTROLLER_X) minX = CONTROLLER_X;
        if (minZ > CONTROLLER_Z) minZ = CONTROLLER_Z;

        List<StructureLibBuildResult.PlacedBlock> result = new ArrayList<>(filledBlocks.size());
        for (int[] pos : filledBlocks) {
            int x = pos[0], y = pos[1], z = pos[2];
            Block block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) continue;

            int meta = level.getBlockMetadata(x, y, z);
            TileEntity tile = level.getTileEntity(x, y, z);
            String blockId = resolvePlacedBlockId(level, x, y, z, block);

            result.add(
                new StructureLibBuildResult.PlacedBlock(
                    x - minX,
                    y - minY,
                    z - minZ,
                    block,
                    meta,
                    serializeTile(tile),
                    blockId));
        }

        result.sort(
            Comparator.comparingInt(StructureLibBuildResult.PlacedBlock::x)
                .thenComparingInt(StructureLibBuildResult.PlacedBlock::y)
                .thenComparingInt(StructureLibBuildResult.PlacedBlock::z));
        return result;
    }

    // ========== Utilities ==========

    @Nullable
    public static NBTTagCompound serializeTile(@Nullable TileEntity tile) {
        if (tile == null) return null;
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            return tag;
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static String resolveBlockId(@Nullable Block block) {
        if (block == null) return null;
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        if (uid != null) return uid.toString();
        Object name = Block.blockRegistry.getNameForObject(block);
        return name != null ? normalizeBlockId(name.toString()) : null;
    }

    @Nullable
    public static String normalizeBlockId(@Nullable String blockId) {
        if (blockId == null) return null;
        String trimmed = blockId.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("tile.") && trimmed.length() > 5) return "minecraft:" + trimmed.substring(5);
        int idx = trimmed.indexOf(":tile.");
        if (idx >= 0) return trimmed.substring(0, idx + 1) + trimmed.substring(idx + 6);
        return trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
    }

    @Nullable
    private static String resolvePlacedBlockId(GuidebookLevel level, int x, int y, int z, Block block) {
        String explicit = level.getExplicitBlockId(x, y, z);
        return explicit != null ? explicit : resolveBlockId(block);
    }

    public static IAlignment resolveAlignment(TileEntity tile) {
        return StructureLibOrientationHelper.resolveAlignment(tile);
    }
}
