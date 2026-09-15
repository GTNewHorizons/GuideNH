package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;
import com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import blockrenderer6343.client.utils.ConstructableData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * Converts StructureLib's public element-visit instrumentation into scene hover metadata.
 * The candidates are resolved through {@link IStructureElement#getBlocksToPlace}, the same
 * API StructureLib uses for survival construction; no element implementation is inspected.
 */
final class StructureLibPreviewTooltipMetadataBuilder {

    private static final String STRUCTURELIB_DESCRIPTION = "StructureLib";
    private static final IItemSource EMPTY_ITEM_SOURCE = (predicate, simulate, count) -> Map.of();

    private StructureLibPreviewTooltipMetadataBuilder() {}

    /**
     * Adds the per-block tooltips to metadata that already carries the controller's tier and channel ranges.
     * Separating the two lets a failure here leave those ranges, and so the sliders, intact.
     */
    static StructureLibSceneMetadata build(StructureLibSceneMetadata metadata, StructureLibBuildRequest request,
        List<StructureLibBuildResult.PlacedBlock> blocks, Long2ObjectMap<IStructureElement<?>> visitedElements,
        int originX, int originY, int originZ, Object context, World world, ItemStack trigger, EntityPlayer actor,
        List<ItemStack> machineStacks) {
        if (blocks.isEmpty() || visitedElements.isEmpty()) {
            return metadata;
        }

        Map<Long, StructureLibSceneMetadata.BlockTooltipData> tooltipData = new LinkedHashMap<>();
        List<ItemStack> candidates = collectMachineCandidates(machineStacks);
        Resolution resolution = Resolution.create();
        for (StructureLibBuildResult.PlacedBlock block : blocks) {
            int worldX = block.x() + originX;
            int worldY = block.y() + originY;
            int worldZ = block.z() + originZ;
            IStructureElement<?> element = visitedElements
                .get(StructureLibSceneMetadata.packBlockPos(worldX, worldY, worldZ));
            if (element == null) {
                continue;
            }
            StructureLibSceneMetadata.BlockTooltipData data = resolve(
                element,
                context,
                world,
                worldX,
                worldY,
                worldZ,
                trigger,
                actor,
                candidates,
                resolution);
            if (data != null && data.hasAdditionalTooltipContent()) {
                tooltipData.put(StructureLibSceneMetadata.packBlockPos(block.x(), block.y(), block.z()), data);
            }
        }
        return metadata.withBlockTooltips(tooltipData);
    }

    /**
     * The tier and channel ranges a controller exposes, read from its structure definition. This is a
     * definition lookup with no world work, which is why a caller can obtain it before, or without, building
     * the structure itself.
     */
    static StructureLibSceneMetadata createControlMetadata(StructureLibBuildRequest request, Object context) {
        return createControlMetadata(
            request.controllerId(),
            request.piece(),
            request.facing(),
            request.rotation(),
            request.flip(),
            context,
            request);
    }

    /**
     * The tier and channel data for a controller.
     *
     * <p>
     * {@code ConstructableData} is keyed by the registered {@link IConstructable} using identity, but placing
     * a controller in the preview world creates a fresh instance, so looking the placed tile up directly
     * always misses and reports no tiers or channels. Both lookups therefore run and the one that actually
     * carries data wins, which is what keeps the tier and channel sliders available.
     */
    @Nullable
    private static ConstructableData resolveControlData(String controllerId, Object context) {
        ConstructableData byId = StructureLibDefinitionCache.getInstance()
            .getConstructableDataFor(controllerId);
        ConstructableData byContext = context instanceof com.gtnewhorizon.structurelib.alignment.constructable.IConstructable constructable
            ? ConstructableData.getTierData(constructable)
            : null;
        GuideDebugLog.warnAlways(
            "[GuideNH] [StructureLib] Resolve {}: byId={}, byContext={}, contextIsConstructable={}",
            controllerId,
            describeData(byId),
            describeData(byContext),
            context instanceof com.gtnewhorizon.structurelib.alignment.constructable.IConstructable);
        if (byId != null && byId.hasData()) {
            return byId;
        }
        if (byContext != null && byContext.hasData()) {
            return byContext;
        }
        // Reporting the data-less result still yields a controller identity, so block tooltips keep working.
        return byId;
    }

    private static String describeData(@Nullable ConstructableData data) {
        if (data == null) {
            return "null";
        }
        return "hasData=" + data.hasData()
            + ",maxTier="
            + data.getMaxTotalTier()
            + ",channels="
            + (data.getChannelData() == null ? -1
                : data.getChannelData()
                    .size());
    }

    private static StructureLibSceneMetadata createControlMetadata(String controller, String piece, String facing,
        String rotation, String flip, Object context, StructureLibBuildRequest request) {
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(controller, piece, facing, rotation, flip);
        ConstructableData data = resolveControlData(controller, context);
        GuideDebugLog.warnAlways(
            "[GuideNH] [StructureLib] Control metadata for {}: data={}, hasData={}, maxTier={}, channels={}",
            controller,
            data == null ? "null"
                : data.getClass()
                    .getSimpleName(),
            data != null && data.hasData(),
            data == null ? -1 : data.getMaxTotalTier(),
            data == null || data.getChannelData() == null ? -1
                : data.getChannelData()
                    .size());
        if (data == null) {
            return metadata;
        }
        int maxTier = Math.max(1, data.getMaxTotalTier());
        List<StructureLibSceneMetadata.ChannelData> channels = new ArrayList<>();
        if (data.getChannelData() == null) {
            return metadata.withTierAndChannelData(1, maxTier, request.tier(), request.tier(), channels);
        }
        for (var entry : data.getChannelData()
            .object2IntEntrySet()) {
            String channel = StructureLibPreviewSelection.normalizeChannelId(entry.getKey());
            if (channel != null) {
                channels.add(
                    new StructureLibSceneMetadata.ChannelData(
                        channel,
                        channel,
                        entry.getIntValue(),
                        0,
                        request.channels()
                            .getOrDefault(channel, 0)));
            }
        }
        return metadata.withTierAndChannelData(1, maxTier, request.tier(), request.tier(), channels);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StructureLibSceneMetadata.BlockTooltipData resolve(IStructureElement<?> element, Object context,
        World world, int x, int y, int z, ItemStack trigger, EntityPlayer actor, List<ItemStack> machineStacks,
        Resolution resolution) {
        try {
            if (element instanceof IStructureElementChain<?>chain) {
                return resolveChain(chain, context, world, x, y, z, trigger, actor, machineStacks, resolution);
            }
            return resolveSingle(element, context, world, x, y, z, trigger, actor, machineStacks, resolution);
        } catch (RuntimeException e) {
            if (resolution.failedElements()
                .add(element.getClass())) {
                GuideDebugLog.warn(
                    "[GuideNH] [StructureLib] Candidate query failed: element={}, context={}, position=({}, {}, {}); other elements remain available",
                    element.getClass()
                        .getName(),
                    context == null ? "null"
                        : context.getClass()
                            .getName(),
                    x,
                    y,
                    z,
                    e);
            }
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StructureLibSceneMetadata.BlockTooltipData resolveSingle(IStructureElement<?> element,
        Object context, World world, int x, int y, int z, ItemStack trigger, EntityPlayer actor,
        List<ItemStack> machineStacks, Resolution resolution) {
        IStructureElement.BlocksToPlace blocksToPlace = ((IStructureElement) element).getBlocksToPlace(
            context,
            world,
            x,
            y,
            z,
            trigger,
            AutoPlaceEnvironment.fromLegacy(EMPTY_ITEM_SOURCE, actor, ignored -> {}));
        if (blocksToPlace == null) {
            return new StructureLibSceneMetadata.BlockTooltipData(
                STRUCTURELIB_DESCRIPTION,
                List.of(),
                List.of(),
                List.of());
        }

        // Both candidate lists come from the element's own placement rule, which does not vary between the
        // positions that element occupies, so each is resolved and detached once per element.
        List<ItemStack> blockCandidates = new ArrayList<>(
            cachedBlocks(element, blocksToPlace, resolution.blockCache()));
        List<ItemStack> hatchCandidates = new ArrayList<>(
            cachedHatches(element, blocksToPlace, machineStacks, resolution.hatchCache()));
        if (!hatchCandidates.isEmpty()) {
            // Explicit non-hatch machine blocks are fixed structure parts, not replaceable hatch positions.
            // Modules accepted only by the hatch predicate must still be shown even though they are not MTEHatch.
            hatchCandidates.removeIf(
                stack -> !GregTechHelpers.isMTEHatch(GregTechHelpers.getMetaTileEntityFromItem(stack))
                    && containsStack(blockCandidates, stack));
            blockCandidates.removeIf(stack -> containsStack(hatchCandidates, stack));
        }
        List<StructureLibHatchDescriptionLine> hatchLines = hatchCandidates.isEmpty() ? List.of()
            : resolution.descriptions()
                .computeIfAbsent(element, key -> describe(key, context));
        return new StructureLibSceneMetadata.BlockTooltipData(
            STRUCTURELIB_DESCRIPTION,
            blockCandidates,
            hatchLines,
            hatchCandidates,
            true);
    }

    /**
     * State shared by one metadata build. The hatch cache is what keeps a build from re-testing the whole
     * meta tile entity registry for every block of a structure; see {@link #cachedHatches}.
     */
    private record Resolution(Set<Class<?>> failedElements,
        Map<IStructureElement<?>, List<StructureLibHatchDescriptionLine>> descriptions,
        Map<IStructureElement<?>, List<ItemStack>> hatchCache, Map<IStructureElement<?>, List<ItemStack>> blockCache) {

        static Resolution create() {
            return new Resolution(
                new HashSet<>(),
                new IdentityHashMap<>(),
                new IdentityHashMap<>(),
                new IdentityHashMap<>());
        }
    }

    /**
     * The block candidates for one element, resolved at most once per build.
     *
     * <p>
     * A structure visits the same element at every position it occupies, and detaching each candidate is
     * expensive because copying an {@link ItemStack} makes Forge collect its capabilities. The list is keyed
     * by element for the same reason as {@link #cachedHatches}: the element is what the placement rule
     * belongs to.
     */
    private static List<ItemStack> cachedBlocks(IStructureElement<?> element,
        IStructureElement.BlocksToPlace blocksToPlace, Map<IStructureElement<?>, List<ItemStack>> blockCache) {
        if (blocksToPlace == null) {
            return List.of();
        }
        List<ItemStack> cached = blockCache.get(element);
        if (cached != null) {
            return cached;
        }
        List<ItemStack> resolved = List.copyOf(normalize(blocksToPlace.getStacks()));
        blockCache.put(element, resolved);
        return resolved;
    }

    /**
     * The hatch candidates for one element, resolved at most once per build.
     *
     * <p>
     * A structure visits the same element at every position it occupies, and both halves of the query are
     * expensive: {@code getBlocksToPlace} rebuilds the hatch rule, which streams the element's declared hatch
     * list, and testing that rule walks the whole meta tile entity registry. The element is the stable
     * identity to key on; its predicate is not, because {@code HatchElementBuilder} builds a fresh predicate
     * on every call, so keying on the predicate would never hit and would retain one entry per block.
     */
    private static List<ItemStack> cachedHatches(IStructureElement<?> element,
        IStructureElement.BlocksToPlace blocksToPlace, List<ItemStack> machineStacks,
        Map<IStructureElement<?>, List<ItemStack>> hatchCache) {
        if (blocksToPlace == null || machineStacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> cached = hatchCache.get(element);
        if (cached != null) {
            return cached;
        }
        List<ItemStack> resolved = List.copyOf(resolveHatches(blocksToPlace.getPredicate(), machineStacks));
        hatchCache.put(element, resolved);
        return resolved;
    }

    /**
     * A HatchElementBuilder followed by {@code buildAndChain(casing)} is visited as one chain.
     * Resolve each public fallback separately so a failing branch cannot hide the other candidates,
     * and descriptions remain associated with their hatch branch instead of the casing fallback.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StructureLibSceneMetadata.BlockTooltipData resolveChain(IStructureElementChain<?> chain,
        Object context, World world, int x, int y, int z, ItemStack trigger, EntityPlayer actor,
        List<ItemStack> machineStacks, Resolution resolution) {
        IStructureElement<?>[] fallbacks = chain.fallbacks();
        if (fallbacks == null || fallbacks.length == 0) {
            return new StructureLibSceneMetadata.BlockTooltipData(
                STRUCTURELIB_DESCRIPTION,
                List.of(),
                List.of(),
                List.of());
        }

        List<ItemStack> blockCandidates = new ArrayList<>();
        List<StructureLibHatchDescriptionLine> hatchLines = new ArrayList<>();
        List<ItemStack> hatchCandidates = new ArrayList<>();
        for (IStructureElement<?> fallback : fallbacks) {
            if (fallback == null) {
                continue;
            }
            StructureLibSceneMetadata.BlockTooltipData data = resolve(
                fallback,
                context,
                world,
                x,
                y,
                z,
                trigger,
                actor,
                machineStacks,
                resolution);
            if (data == null) {
                continue;
            }
            blockCandidates.addAll(data.getBlockCandidates());
            hatchLines.addAll(data.getHatchDescriptionLines());
            hatchCandidates.addAll(data.getHatchCandidates());
        }

        // The fallbacks already handed over detached stacks, so only their identity is decided here; a
        // further normalize would re-run Forge's capability collection for every stack of every fallback.
        List<ItemStack> normalizedHatches = deduplicate(hatchCandidates);
        if (!normalizedHatches.isEmpty()) {
            blockCandidates.removeIf(stack -> containsStack(normalizedHatches, stack));
        }
        return new StructureLibSceneMetadata.BlockTooltipData(
            STRUCTURELIB_DESCRIPTION,
            deduplicate(blockCandidates),
            hatchLines,
            normalizedHatches,
            true);
    }

    private static List<ItemStack> resolveHatches(Predicate<ItemStack> predicate, List<ItemStack> machineStacks) {
        if (predicate == null || machineStacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> matches = new ArrayList<>();
        for (ItemStack stack : machineStacks) {
            if (predicate.test(stack)) {
                matches.add(copyUnit(stack));
            }
        }
        return matches;
    }

    private static boolean containsStack(List<ItemStack> candidates, ItemStack stack) {
        for (ItemStack candidate : candidates) {
            if (ItemStack.areItemStacksEqual(candidate, stack)) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> collectMachineCandidates(List<ItemStack> machineStacks) {
        if (machineStacks == null || machineStacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> candidates = new ArrayList<>();
        // IHatchElement can accept module controllers as well as MTEHatch implementations.
        for (ItemStack stack : machineStacks) {
            if (stack != null && stack.getItem() != null) {
                candidates.add(stack);
            }
        }
        return candidates.isEmpty() ? List.of() : List.copyOf(candidates);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<StructureLibHatchDescriptionLine> describe(IStructureElement<?> element, Object context) {
        try {
            List<String> descriptions = ((IStructureElement) element).getDescription(context);
            if (descriptions == null || descriptions.isEmpty()) {
                return List.of();
            }
            List<String> filtered = descriptions.stream()
                .filter(
                    value -> value != null && !value.trim()
                        .isEmpty())
                .map(StatCollector::translateToLocal)
                .toList();
            return filtered.isEmpty() ? List.of()
                : List.of(StructureLibHatchDescriptionLine.validHatches(String.join(" / ", filtered)));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static List<ItemStack> normalize(Iterable<ItemStack> stacks) {
        if (stacks == null) {
            return new ArrayList<>();
        }
        Map<String, ItemStack> deduplicated = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack != null && stack.getItem() != null) {
                ItemStack copy = copyUnit(stack);
                deduplicated.putIfAbsent(
                    copy.getItem()
                        .getUnlocalizedName() + ':'
                        + copy.getItemDamage(),
                    copy);
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    /**
     * Keeps one stack per item and damage value, without detaching them.
     *
     * <p>
     * Used where the stacks already came from {@link #normalize} or from another resolve, so copying them
     * again would only repeat Forge's capability collection. The identity of the surviving stack is what the
     * caller stores, which is why this may share instances.
     */
    private static List<ItemStack> deduplicate(Iterable<ItemStack> stacks) {
        Map<String, ItemStack> deduplicated = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack != null && stack.getItem() != null) {
                deduplicated.putIfAbsent(
                    stack.getItem()
                        .getUnlocalizedName() + ':'
                        + stack.getItemDamage(),
                    stack);
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    private static ItemStack copyUnit(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }
}
