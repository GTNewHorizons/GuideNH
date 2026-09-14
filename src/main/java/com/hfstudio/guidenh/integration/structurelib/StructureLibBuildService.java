package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
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
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import blockrenderer6343.api.utils.CreativeItemSource;
import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;

public class StructureLibBuildService {

    public static final int CONTROLLER_X = 0;
    public static final int CONTROLLER_Y = 64;
    public static final int CONTROLLER_Z = 0;
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 50;
    public static final int SURVIVAL_BUDGET = Integer.MAX_VALUE;
    public static final int SURVIVAL_MAX_ROUNDS = 256;

    public record ResolvedController(String blockId, Block block, int meta) {}

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

    public static ResolvedController resolveController(String controllerId) {
        GuideBlockMatcher matcher = GuideBlockMatcher.parse(controllerId);
        Block block = (Block) Block.blockRegistry.getObject(matcher.getBlockId());
        if (block == null || block == Blocks.air) {
            throw new IllegalArgumentException("Could not resolve controller block: " + controllerId);
        }
        return new ResolvedController(matcher.getBlockId(), block, matcher.getMeta() != null ? matcher.getMeta() : 0);
    }

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

    public static void buildStructure(IConstructable constructable, ItemStack trigger, PreviewFakePlayer fakePlayer,
        StructureLibBuildRequest request, TileEntity controllerTile) {
        GregTechHelpers.resolvePreviewModifier(controllerTile, trigger, true);
        boolean useSurvival = constructable instanceof ISurvivalConstructable;
        if (useSurvival) {
            ISurvivalConstructable sc = (ISurvivalConstructable) constructable;
            ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(createItemSource(), fakePlayer);
            int rounds = 0;
            while (rounds++ < SURVIVAL_MAX_ROUNDS) {
                int result = sc.survivalConstruct(trigger, SURVIVAL_BUDGET, env);
                if (result == -1) {
                    GregTechHelpers.resolvePreviewModifier(controllerTile, trigger, false);
                    return; // success
                }
                if (result == -2) {
                    GuideDebugLog.warn(
                        "[GuideNH] [StructureLib] Survival preview requested creative fallback: controller={}, round={}",
                        request.controllerId(),
                        rounds);
                    break;
                }
                if (result <= 0) {
                    GuideDebugLog.warn(
                        "[GuideNH] [StructureLib] Survival preview stopped without progress: controller={}, round={}, result={}",
                        request.controllerId(),
                        rounds,
                        result);
                    break;
                }
                GregTechHelpers.refreshPreviewHatchList(controllerTile, trigger, null);
            }
        }
        constructable.construct(trigger.copy(), false);
        GregTechHelpers.resolvePreviewModifier(controllerTile, trigger, false);
    }

    public static IItemSource createItemSource() {
        return PreviewItemSourceHolder.INSTANCE;
    }

    /** Lazily initializes after GregTech has registered its meta tile entities. */
    private static final class PreviewItemSourceHolder {

        private static final IItemSource INSTANCE = createPreviewItemSource();

        private static IItemSource createPreviewItemSource() {
            List<ItemStack> hiddenGregTechItems = new ArrayList<>();
            if (Mods.GregTech.isModLoaded()) {
                GregTechHelpers.appendMachineStacks(hiddenGregTechItems);
            }
            return hiddenGregTechItems.isEmpty() ? CreativeItemSource.instance
                : new FallbackItemSource(
                    CreativeItemSource.instance,
                    new CachedCreativeItemSource(hiddenGregTechItems));
        }
    }

    /**
     * NEI omits some valid meta tile entities, including hidden hatches. StructureLib's
     * placement predicates still need to see every registered GregTech MTE in preview mode.
     */
    public static class FallbackItemSource implements IItemSource {

        public final IItemSource primary;
        public final IItemSource fallback;

        public FallbackItemSource(IItemSource primary, IItemSource fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Nonnull
        @Override
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            Map<ItemStack, Integer> result = primary.take(predicate, simulate, count);
            return result.isEmpty() ? fallback.take(predicate, simulate, count) : result;
        }

        @Override
        public ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
            ItemStack result = primary.takeOne(predicate, simulate);
            return result != null ? result : fallback.takeOne(predicate, simulate);
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            return primary.takeOne(stack, simulate) || fallback.takeOne(stack, simulate);
        }

        @Override
        public boolean takeAll(ItemStack stack, boolean simulate) {
            return primary.takeAll(stack, simulate) || fallback.takeAll(stack, simulate);
        }
    }

    /**
     * Supplies all registered GregTech MTEs, including hatches deliberately hidden from
     * NEI. Successful predicate matches are checked first on later placement attempts,
     * matching BlockRenderer6343's creative item-source cache.
     */
    public static class CachedCreativeItemSource implements IItemSource {

        public final List<ItemStack> itemList;
        private final Reference2ReferenceLinkedOpenHashMap<ItemStack, ItemStack> recentMatches = new Reference2ReferenceLinkedOpenHashMap<>();

        public CachedCreativeItemSource(List<ItemStack> itemList) {
            this.itemList = itemList;
        }

        @Nonnull
        @Override
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            ItemStack itemStack = takeOne(predicate, simulate);
            return itemStack != null ? Collections.singletonMap(itemStack, Integer.MAX_VALUE) : Collections.emptyMap();
        }

        @Override
        public ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
            for (ItemStack itemStack : recentMatches.values()) {
                if (predicate.test(itemStack)) {
                    return itemStack;
                }
            }
            for (ItemStack itemStack : itemList) {
                if (predicate.test(itemStack)) {
                    recentMatches.put(itemStack, itemStack);
                    return itemStack;
                }
            }
            return null;
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            return true;
        }

        @Override
        public boolean takeAll(ItemStack stack, boolean simulate) {
            return true;
        }
    }

    public static void syncPreviewState(TileEntity controllerTile, ItemStack trigger,
        StructureLibBuildRequest request) {
        for (StructureLibPreviewStateSynchronizer synchronizer : StructureLibControllerIntegrationRegistry.global()
            .previewStateSynchronizers()) {
            synchronizer.synchronizePreviewState(controllerTile, trigger, request);
        }
    }

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
