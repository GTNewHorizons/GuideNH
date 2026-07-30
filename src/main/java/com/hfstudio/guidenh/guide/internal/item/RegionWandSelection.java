package com.hfstudio.guidenh.guide.internal.item;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;
import com.hfstudio.guidenh.config.ModConfig;

public class RegionWandSelection {

    @Nullable
    private static BlockPos pos1;
    @Nullable
    private static BlockPos pos2;
    private static volatile BindingCache bindingCache = BindingCache.empty();
    private static volatile boolean bindingsInitialized;

    private RegionWandSelection() {}

    public static void setPos(int which, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (which == 1) {
            pos1 = pos;
        } else if (which == 2) {
            pos2 = pos;
        }
    }

    @Nullable
    public static IBlockPos getPosition(int which) {
        return which == 1 ? pos1 : which == 2 ? pos2 : null;
    }

    public static boolean hasCompleteSelection() {
        return pos1 != null && pos2 != null;
    }

    public static void clear() {
        pos1 = null;
        pos2 = null;
    }

    public static boolean bind(ItemStack stack) {
        String key = bindingKey(stack);
        if (key == null) {
            return false;
        }
        ensureBindingsInitialized();
        if (bindingCache.keys()
            .size() == 1 && bindingCache.keys()
                .contains(key)) {
            return false;
        }
        saveBinding(key);
        return true;
    }

    public static boolean clearBinding() {
        ensureBindingsInitialized();
        if (bindingCache.keys()
            .isEmpty()) {
            return false;
        }
        saveBinding(null);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        ensureBindingsInitialized();
        BitSet metadata = bindingCache.metadataByItem()
            .get(stack.getItem());
        int itemDamage = stack.getItemDamage();
        return metadata != null && itemDamage >= 0 && metadata.get(itemDamage);
    }

    public static synchronized void reloadBindings() {
        String binding = null;
        String[] configuredBindings = ModConfig.ui.regionWandBindings;
        if (configuredBindings != null) {
            for (String candidate : configuredBindings) {
                if (candidate != null && !candidate.isEmpty()) {
                    binding = candidate;
                    break;
                }
            }
        }
        bindingCache = createBindingCache(binding != null ? Set.of(binding) : Set.of());
        bindingsInitialized = true;
    }

    private static String bindingKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        return itemId + "@" + stack.getItemDamage();
    }

    private static void ensureBindingsInitialized() {
        if (!bindingsInitialized) {
            reloadBindings();
        }
    }

    private static void saveBinding(@Nullable String binding) {
        Set<String> keys = binding != null ? Set.of(binding) : Set.of();
        ModConfig.ui.regionWandBindings = keys.toArray(new String[0]);
        bindingCache = createBindingCache(keys);
        bindingsInitialized = true;
        ModConfig.save();
    }

    private static BindingCache createBindingCache(Set<String> keys) {
        Map<Item, BitSet> metadataByItem = new HashMap<>();
        for (String key : keys) {
            int separator = key.lastIndexOf('@');
            if (separator <= 0 || separator == key.length() - 1) {
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(key.substring(0, separator));
            if (item == null) {
                continue;
            }
            try {
                int metadata = Integer.parseInt(key.substring(separator + 1));
                if (metadata >= 0) {
                    metadataByItem.computeIfAbsent(item, ignored -> new BitSet())
                        .set(metadata);
                }
            } catch (NumberFormatException ignored) {}
        }
        return new BindingCache(Map.copyOf(metadataByItem), Set.copyOf(keys));
    }

    @Nullable
    public static Bounds getBounds() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Desugar
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public int sizeX() {
            return maxX - minX + 1;
        }

        public int sizeY() {
            return maxY - minY + 1;
        }

        public int sizeZ() {
            return maxZ - minZ + 1;
        }
    }

    @Desugar
    private record BindingCache(Map<Item, BitSet> metadataByItem, Set<String> keys) {

        private static BindingCache empty() {
            return new BindingCache(Map.of(), Set.of());
        }
    }
}
