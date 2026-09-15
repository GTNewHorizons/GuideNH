package com.hfstudio.guidenh.guide.scene.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343.AccessorConstructableData;

import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.integration.gregtech.GTConstructableScan;
import blockrenderer6343.integration.structurelib.MultiblockInfoContainerScan;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * Resolves the tier and channel ranges BlockRenderer6343 found for a controller.
 *
 * <p>
 * Those ranges only decide whether the sliders appear, never whether a machine can be rendered. Machine
 * discovery uses {@code GregTechAPI.METATILEENTITIES}, the full unfiltered list, while the ranges come from
 * {@code ConstructableData}, which only covers the machines that expose tiers or channels.
 */
public class StructureLibDefinitionCache {

    private static final StructureLibDefinitionCache INSTANCE = new StructureLibDefinitionCache();
    private final Map<String, IConstructable> scannedControllers = new ConcurrentHashMap<>();
    private final Map<String, IConstructable> resolvedControllers = new ConcurrentHashMap<>();
    private final Map<String, ConstructableData> resolvedData = new ConcurrentHashMap<>();
    private volatile boolean scanRequested;

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
    }

    public synchronized void startScans() {
        if (scanRequested || !Mods.BlockRenderer6343.isModLoaded()) return;
        if (hasPublishedData()) {
            scanRequested = true;
            return;
        }
        scanRequested = true;
        // Run the scanners directly after all mods have registered. This also repairs installations where
        // NEI initialized its handlers before the registry was complete; loading the handler class again would
        // not rerun its static initializer.
        GuideDebugLog.info("[GuideNH] [StructureLib] Starting post-registration definition scans");
        Runnable structureLibScan = new MultiblockInfoContainerScan(ignored -> {}, stacks -> {
            indexScannedControllers(stacks);
            GuideDebugLog.info("[GuideNH] [StructureLib] Container scan published {} controller stacks", stacks.size());
        }, IMultiblockInfoContainer.MULTIBLOCK_MAP);
        new Thread(structureLibScan, "GuideNH-StructureLibScan").start();
        if (Mods.GregTech.isModLoaded()) {
            List<IConstructable> constructables = new ArrayList<>();
            for (IMetaTileEntity mte : GregTechAPI.METATILEENTITIES) {
                if (mte instanceof IConstructable c) constructables.add(c);
            }
            Runnable gregTechScan = new GTConstructableScan(
                result -> GuideDebugLog
                    .info("[GuideNH] [StructureLib] GregTech scan published {} stack buckets", result.size()),
                constructables);
            new Thread(gregTechScan, "GuideNH-GregTechStructureScan").start();
        }
    }

    private static boolean hasPublishedData() {
        try {
            return !AccessorConstructableData.getConstructableDataMap()
                .isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void indexScannedControllers(Map<IConstructable, ItemStack> stacks) {
        for (Map.Entry<IConstructable, ItemStack> entry : stacks.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack == null || stack.getItem() == null) continue;
            String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
            if (itemId == null) continue;
            scannedControllers.put(itemId, entry.getKey());
            scannedControllers.put(itemId + ":" + stack.getItemDamage(), entry.getKey());
            resolvedControllers.put(itemId, entry.getKey());
            resolvedControllers.put(itemId + ":" + stack.getItemDamage(), entry.getKey());
        }
    }

    /**
     * Finds the {@code IConstructable} a controller id refers to.
     *
     * <p>
     * Iterates {@code GregTechAPI.METATILEENTITIES}, the full unfiltered list of registered machines, so this
     * works for the machines that carry no tier data as well. The registry holds the instances BlockRenderer6343
     * keys its data on, which is why the id is resolved through it rather than through the placed controller:
     * placing one in the preview world builds a fresh instance that its identity-keyed map cannot match.
     */
    @Nullable
    public IConstructable findConstructable(String controllerBlockId) {
        if (controllerBlockId == null || controllerBlockId.isEmpty()) return null;
        IConstructable cached = resolvedControllers.get(controllerBlockId);
        if (cached != null) return cached;
        try {
            for (IMetaTileEntity mte : GregTechAPI.METATILEENTITIES) {
                if (mte instanceof IConstructable c && isControllerMatch(c, controllerBlockId)) {
                    resolvedControllers.put(controllerBlockId, c);
                    return c;
                }
            }
        } catch (Throwable _) {}
        return null;
    }

    private static boolean isControllerMatch(IConstructable c, String blockId) {
        if (c instanceof IMetaTileEntity mte) {
            ItemStack stack = mte.getStackForm(1);
            if (stack == null || stack.getItem() == null) return false;
            String id = Item.itemRegistry.getNameForObject(stack.getItem());
            if (id == null) return false;
            int damage = stack.getItemDamage();
            return (id + ":" + damage).equals(blockId) || id.equals(blockId);
        }
        return false;
    }

    /**
     * The tier and channel ranges of a machine, or an empty default when it exposes none.
     */
    public ConstructableData getConstructableData(IConstructable c) {
        ConstructableData direct = ConstructableData.getTierData(c);
        if (direct.hasData()) return direct;
        try {
            var map = AccessorConstructableData.getConstructableDataMap();
            synchronized (map) {
                ConstructableData data = map.get(c);
                if (data != null && data.hasData()) return data;
                // ConstructableData is identity-keyed. StructureLib container scans may create an equivalent
                // constructable instance, so match the published entries by their controller stack as a fallback.
                for (var entry : map.object2ObjectEntrySet()) {
                    if (isSameController(entry.getKey(), c)) {
                        return entry.getValue();
                    }
                }
            }
        } catch (Throwable ignored) {
            // The accessor is only available when BlockRenderer6343's compatibility mixin is applied.
        }
        return ConstructableData.getTierData(c);
    }

    @Nullable
    public ConstructableData getConstructableDataFor(String controllerBlockId) {
        ConstructableData cachedData = resolvedData.get(controllerBlockId);
        if (cachedData != null) return cachedData;
        IConstructable scanned = scannedControllers.get(controllerBlockId);
        if (scanned != null) return rememberData(controllerBlockId, getConstructableData(scanned));
        IConstructable c = findConstructable(controllerBlockId);
        if (c != null) return rememberData(controllerBlockId, getConstructableData(c));

        // Non-GregTech StructureLib controllers are not present in METATILEENTITIES; resolve them directly
        // from the published map by controller item id.
        try {
            var map = AccessorConstructableData.getConstructableDataMap();
            synchronized (map) {
                for (var entry : map.object2ObjectEntrySet()) {
                    if (isControllerMatch(entry.getKey(), controllerBlockId)) {
                        resolvedControllers.put(controllerBlockId, entry.getKey());
                        return rememberData(controllerBlockId, entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private ConstructableData rememberData(String controllerBlockId, ConstructableData data) {
        if (data != null && data.hasData()) {
            resolvedData.putIfAbsent(controllerBlockId, data);
        }
        return data;
    }

    private static boolean isSameController(IConstructable a, IConstructable b) {
        if (!(a instanceof IMetaTileEntity ma) || !(b instanceof IMetaTileEntity mb)) return a == b;
        ItemStack sa = ma.getStackForm(1);
        ItemStack sb = mb.getStackForm(1);
        return sa != null && sb != null && sa.getItem() == sb.getItem() && sa.getItemDamage() == sb.getItemDamage();
    }
}
