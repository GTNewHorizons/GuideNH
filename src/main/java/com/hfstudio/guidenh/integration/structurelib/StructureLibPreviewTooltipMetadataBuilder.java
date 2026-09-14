package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import blockrenderer6343.client.utils.ConstructableData;

/**
 * Converts StructureLib's public element-visit instrumentation into scene hover metadata.
 * The candidates are resolved through {@link IStructureElement#getBlocksToPlace}, the same
 * API StructureLib uses for survival construction; no element implementation is inspected.
 */
final class StructureLibPreviewTooltipMetadataBuilder {

    private static final String STRUCTURELIB_DESCRIPTION = "StructureLib";
    private static final IItemSource EMPTY_ITEM_SOURCE = (predicate, simulate, count) -> Map.of();

    private StructureLibPreviewTooltipMetadataBuilder() {}

    static StructureLibSceneMetadata build(StructureLibBuildRequest request,
        List<StructureLibBuildResult.PlacedBlock> blocks, Map<Long, IStructureElement<?>> visitedElements, int originX,
        int originY, int originZ, Object context, World world, ItemStack trigger, EntityPlayer actor,
        List<ItemStack> machineStacks) {
        StructureLibSceneMetadata metadata = createControlMetadata(
            request.controllerId(),
            request.piece(),
            request.facing(),
            request.rotation(),
            request.flip(),
            context,
            request);
        if (blocks.isEmpty() || visitedElements.isEmpty()) {
            return metadata;
        }

        Map<Long, StructureLibSceneMetadata.BlockTooltipData> tooltipData = new LinkedHashMap<>();
        List<ItemStack> hatchStacks = collectHatchStacks(machineStacks);
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
                hatchStacks);
            if (data != null && data.hasAdditionalTooltipContent()) {
                tooltipData.put(StructureLibSceneMetadata.packBlockPos(block.x(), block.y(), block.z()), data);
            }
        }
        return metadata.withBlockTooltips(tooltipData);
    }

    private static StructureLibSceneMetadata createControlMetadata(String controller, String piece, String facing,
        String rotation, String flip, Object context, StructureLibBuildRequest request) {
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(controller, piece, facing, rotation, flip);
        if (!(context instanceof com.gtnewhorizon.structurelib.alignment.constructable.IConstructable constructable)) {
            return metadata;
        }
        ConstructableData data = ConstructableData.getTierData(constructable);
        if (data == null) {
            return metadata;
        }
        int maxTier = Math.max(1, data.getMaxTotalTier());
        metadata = metadata.withTierData(1, maxTier, request.tier(), request.tier());
        if (data.getChannelData() == null) {
            return metadata;
        }
        for (var entry : data.getChannelData()
            .object2IntEntrySet()) {
            String channel = StructureLibPreviewSelection.normalizeChannelId(entry.getKey());
            if (channel != null) {
                metadata = metadata.withChannelData(
                    channel,
                    channel,
                    entry.getIntValue(),
                    request.channels()
                        .getOrDefault(channel, 0));
            }
        }
        return metadata;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StructureLibSceneMetadata.BlockTooltipData resolve(IStructureElement<?> element, Object context,
        World world, int x, int y, int z, ItemStack trigger, EntityPlayer actor, List<ItemStack> machineStacks) {
        IStructureElement.BlocksToPlace blocksToPlace;
        try {
            blocksToPlace = ((IStructureElement) element).getBlocksToPlace(
                context,
                world,
                x,
                y,
                z,
                trigger,
                AutoPlaceEnvironment.fromLegacy(EMPTY_ITEM_SOURCE, actor, ignored -> {}));
        } catch (RuntimeException ignored) {
            return new StructureLibSceneMetadata.BlockTooltipData(
                STRUCTURELIB_DESCRIPTION,
                List.of(),
                List.of(),
                List.of());
        }
        if (blocksToPlace == null) {
            return new StructureLibSceneMetadata.BlockTooltipData(
                STRUCTURELIB_DESCRIPTION,
                List.of(),
                List.of(),
                List.of());
        }

        List<ItemStack> blockCandidates = normalize(blocksToPlace.getStacks());
        List<ItemStack> hatchCandidates = resolveHatches(blocksToPlace.getPredicate(), machineStacks);
        if (!hatchCandidates.isEmpty()) {
            blockCandidates
                .removeIf(stack -> GregTechHelpers.isMTEHatch(GregTechHelpers.getMetaTileEntityFromItem(stack)));
        }
        List<StructureLibHatchDescriptionLine> hatchLines = hatchCandidates.isEmpty() ? List.of()
            : descriptions(element, context);
        return new StructureLibSceneMetadata.BlockTooltipData(
            STRUCTURELIB_DESCRIPTION,
            blockCandidates,
            hatchLines,
            hatchCandidates);
    }

    private static List<ItemStack> resolveHatches(Predicate<ItemStack> predicate, List<ItemStack> hatchStacks) {
        if (predicate == null || hatchStacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> matches = new ArrayList<>();
        for (ItemStack stack : hatchStacks) {
            if (predicate.test(stack)) {
                matches.add(copyUnit(stack));
            }
        }
        return matches.isEmpty() ? List.of() : List.copyOf(matches);
    }

    private static List<ItemStack> collectHatchStacks(List<ItemStack> machineStacks) {
        if (machineStacks == null || machineStacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> hatches = new ArrayList<>();
        for (ItemStack stack : machineStacks) {
            if (stack != null && GregTechHelpers.isMTEHatch(GregTechHelpers.getMetaTileEntityFromItem(stack))) {
                hatches.add(stack);
            }
        }
        return hatches.isEmpty() ? List.of() : List.copyOf(hatches);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<StructureLibHatchDescriptionLine> descriptions(IStructureElement<?> element, Object context) {
        try {
            List<String> descriptions = ((IStructureElement) element).getDescription(context);
            if (descriptions == null || descriptions.isEmpty()) {
                return List.of();
            }
            List<String> filtered = descriptions.stream()
                .filter(
                    value -> value != null && !value.trim()
                        .isEmpty())
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

    private static ItemStack copyUnit(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }
}
