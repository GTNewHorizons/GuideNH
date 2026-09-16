package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraftforge.common.MinecraftForge;
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
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import blockrenderer6343.api.utils.CreativeItemSource;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class StructureLibBuildService {

    public static final int CONTROLLER_X = 0;
    public static final int CONTROLLER_Y = 64;
    public static final int CONTROLLER_Z = 0;
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 50;
    public static final int SURVIVAL_BUDGET = Integer.MAX_VALUE;
    public static final int SURVIVAL_MAX_ROUNDS = 256;

    public record ResolvedController(String blockId, Block block, int meta) {}

    /**
     * A world reused by every build instead of one per build.
     *
     * <p>
     * Creating the preview world is the single most expensive step of a build: {@code GuidebookFakeWorld} is a
     * {@code WorldClient}, so its constructor posts {@code WorldEvent.Load} and every mod listening to it
     * does its world-load work. At least one mod rebuilds the chunk provider of every dimension there, which
     * measured in the seconds. Scene data lives in the level and is dropped by {@code clear()}, while the
     * world itself is designed to outlive rebuilds, so one instance serves all builds.
     *
     * <p>
     * Shared because every caller creates its own service, and confined to the client thread because that is
     * the only thread a build runs on; a build taking it from another thread is reported instead of silently
     * sharing one world.
     */
    private static GuidebookLevel scratchLevel;
    private static Thread scratchLevelOwner;

    private static GuidebookLevel acquireScratchLevel() {
        Thread current = Thread.currentThread();
        GuidebookLevel level = scratchLevel;
        if (level == null) {
            level = new GuidebookLevel();
            scratchLevel = level;
            scratchLevelOwner = current;
        } else if (scratchLevelOwner != current) {
            throw new IllegalStateException(
                "StructureLib preview world is confined to " + scratchLevelOwner.getName()
                    + " but was requested by "
                    + current.getName());
        }
        level.clear();
        return level;
    }

    /**
     * The tier and channel ranges a controller exposes, read from its structure definition.
     *
     * <p>
     * Only a definition lookup, with no world work and no block placement, so a scene built before
     * BlockRenderer6343 published its scan can ask for the ranges again while it is still on screen.
     */
    @Nullable
    public static StructureLibSceneMetadata readControlMetadata(@Nullable StructureLibBuildRequest request) {
        if (request == null) {
            return null;
        }
        return StructureLibPreviewTooltipMetadataBuilder.createControlMetadata(request);
    }

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

        GuidebookLevel level = acquireScratchLevel();
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
        Long2ObjectMap<IStructureElement<?>> visitedElements = Long2ObjectMaps.emptyMap();
        Object instrumentId = new Object();
        StructureLibVisitedElementCollector visitCollector = new StructureLibVisitedElementCollector(
            instrumentId,
            world);
        boolean instrumentEnabled = false;
        boolean instrumentRegistered = false;
        try {
            StructureLibAPI.enableInstrument(instrumentId);
            instrumentEnabled = true;
            MinecraftForge.EVENT_BUS.register(visitCollector);
            instrumentRegistered = true;
        } catch (IllegalStateException e) {
            GuideDebugLog.warn("[GuideNH] [StructureLib] Instrumentation unavailable for {}", request.controllerId());
        }
        try {
            buildStructure(constructable, trigger, fakePlayer, request, controllerTile);
        } finally {
            StructureLibMinimumHatchPlacement.clearCurrentElement();
            if (instrumentRegistered) {
                visitedElements = visitCollector.snapshot();
                MinecraftForge.EVENT_BUS.unregister(visitCollector);
            }
            if (instrumentEnabled) {
                StructureLibAPI.disableInstrument();
            }
        }
        syncPreviewState(controllerTile, trigger, request);
        // Apply explicit NEI preview overrides after the final machine check, which may reset them.
        GregTechHelpers.resolvePreviewModifier(controllerTile, trigger, false);

        BlockSnapshot blockSnapshot = snapshotBlocksAndOrigin(level);
        List<StructureLibBuildResult.PlacedBlock> blocks = blockSnapshot.blocks();
        List<ItemStack> machineStacks = new ArrayList<>();
        GregTechHelpers.appendMachineStacks(machineStacks);
        Object metadataContext = resolveMetadataContext(controllerTile, constructable);
        StructureLibSceneMetadata metadata = StructureLibPreviewTooltipMetadataBuilder.createControlMetadata(request);
        try (StructureLibPreviewMetadataScope ignored = StructureLibPreviewMetadataScope.open()) {
            metadata = StructureLibPreviewTooltipMetadataBuilder.build(
                metadata,
                request,
                blocks,
                visitedElements,
                blockSnapshot.originX(),
                blockSnapshot.originY(),
                blockSnapshot.originZ(),
                metadataContext,
                world,
                trigger,
                fakePlayer,
                machineStacks);
            GuideDebugLog.info(
                "[GuideNH] [StructureLib] Preview metadata: controller={}, blocks={}, visited={}, hatchTooltips={}",
                request.controllerId(),
                blocks.size(),
                visitedElements.size(),
                metadata.getHatchTooltipEntries()
                    .size());
        } catch (Throwable t) {
            // Only the block tooltips are lost here; the tier and channel ranges come from the structure
            // definition and stay valid, so the sliders must not disappear because a tooltip failed.
            GuideDebugLog.warn(
                "[GuideNH] [StructureLib] Preview tooltips failed for {}; tier and channel controls remain available",
                request.controllerId(),
                t);
        }
        return new StructureLibBuildResult(blocks, true, null, metadata);
    }

    /**
     * StructureLib constructables may be lightweight wrappers around the actual GT meta tile entity. Hatch
     * elements are parameterized with that concrete controller type, so metadata queries must use the same object
     * that the structure definition expects rather than the wrapper used to invoke construction.
     */
    private static Object resolveMetadataContext(TileEntity controllerTile, IConstructable constructable) {
        if (controllerTile instanceof IGregTechTileEntity gtTile) {
            IMetaTileEntity metaTileEntity = gtTile.getMetaTileEntity();
            if (metaTileEntity != null) {
                return metaTileEntity;
            }
        }
        return constructable;
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
            boolean creativeFallback = false;
            while (rounds++ < SURVIVAL_MAX_ROUNDS) {
                int result = sc.survivalConstruct(trigger, SURVIVAL_BUDGET, env);
                if (result == -1) {
                    return; // success
                }
                if (result == -2) {
                    GuideDebugLog.warn(
                        "[GuideNH] [StructureLib] Survival preview requested creative fallback: controller={}, round={}",
                        request.controllerId(),
                        rounds);
                    creativeFallback = true;
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
            if (!creativeFallback) {
                GuideDebugLog.warn(
                    "[GuideNH] [StructureLib] Survival preview exceeded the round limit; creative fallback suppressed to preserve optional hatch positions: controller={}, rounds={}",
                    request.controllerId(),
                    rounds);
                creativeFallback = true;
            }
        }
        StructureLibMinimumHatchPlacement.beginCreativeConstruct();
        try {
            constructable.construct(trigger.copy(), false);
        } finally {
            StructureLibMinimumHatchPlacement.endCreativeConstruct();
        }
    }

    public static IItemSource createItemSource() {
        return PreviewItemSourceHolder.INSTANCE;
    }

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
        return snapshotBlocksAndOrigin(level).blocks();
    }

    private static BlockSnapshot snapshotBlocksAndOrigin(GuidebookLevel level) {
        Collection<int[]> filledBlocks = level.getFilledBlocks();
        if (filledBlocks.isEmpty()) {
            return new BlockSnapshot(List.of(), CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        }

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
        // A casing can occupy thousands of positions. Resolve its registry string once per snapshot,
        // while retaining explicit per-position identifiers such as the controller's block id.
        Map<Block, String> blockIds = new Reference2ReferenceOpenHashMap<>();
        for (int[] pos : filledBlocks) {
            int x = pos[0], y = pos[1], z = pos[2];
            Block block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) continue;

            int meta = level.getBlockMetadata(x, y, z);
            TileEntity tile = level.getTileEntity(x, y, z);
            String blockId = level.getExplicitBlockId(x, y, z);
            if (blockId == null) {
                blockId = blockIds.computeIfAbsent(block, StructureLibBuildService::resolveBlockId);
            }

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
        return new BlockSnapshot(result, minX, minY, minZ);
    }

    private record BlockSnapshot(List<StructureLibBuildResult.PlacedBlock> blocks, int originX, int originY,
        int originZ) {}

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

    public static IAlignment resolveAlignment(TileEntity tile) {
        return StructureLibOrientationHelper.resolveAlignment(tile);
    }
}
