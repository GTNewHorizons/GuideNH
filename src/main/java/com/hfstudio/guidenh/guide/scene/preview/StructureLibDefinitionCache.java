package com.hfstudio.guidenh.guide.scene.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.integration.gregtech.GTConstructableScan;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import lombok.Getter;

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
    @Getter
    private volatile boolean scansComplete;
    @Getter
    private volatile long scanGeneration;

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
    }

    public synchronized void startScans() {
        if (scanRequested || !Mods.BlockRenderer6343.isModLoaded()) return;
        scanRequested = true;
        GuideDebugLog.info("[GuideNH] [StructureLib] Starting post-registration definition scans");
        // Both scanners publish into BlockRenderer6343's global ConstructableData map. Run them in one
        // deterministic worker, with the GregTech scan last, so its richer channel/tier data cannot be
        // replaced by the more limited container result for the same controller.
        new Thread(() -> {
            try {
                scanStructureLibContainersSafely();
            } catch (Throwable t) {
                // A compatibility failure in the optional container scan must not prevent the independent
                // GregTech scan from publishing its complete tier/channel data.
                GuideDebugLog.warn("[GuideNH] [StructureLib] Container scan failed", t);
            } finally {
                try {
                    scanGregTechConstructables();
                } catch (Throwable t) {
                    GuideDebugLog.warn("[GuideNH] [StructureLib] GregTech scan failed", t);
                } finally {
                    scansComplete = true;
                    scanGeneration++;
                }
            }
        }, "GuideNH-StructureLibScan").start();
    }

    private void scanStructureLibContainersSafely() {
        new GuideStructureLibContainerScan(ignored -> {}, stacks -> {
            indexScannedControllers(stacks);
            GuideDebugLog.info("[GuideNH] [StructureLib] Container scan published {} controller stacks", stacks.size());
        }, IMultiblockInfoContainer.MULTIBLOCK_MAP).run();
    }

    private void scanGregTechConstructables() {
        if (!Mods.GregTech.isModLoaded()) return;
        List<IConstructable> constructables = new ArrayList<>();
        for (IMetaTileEntity mte : GregTechAPI.METATILEENTITIES) {
            if (mte instanceof IConstructable c) constructables.add(c);
        }
        Runnable gregTechScan = new GTConstructableScan(
            result -> GuideDebugLog
                .info("[GuideNH] [StructureLib] GregTech scan published {} stack buckets", result.size()),
            constructables);
        gregTechScan.run();
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
            // Explicit GT controller metadata is normally its registry slot. Verify the actual stack
            // before accepting it so integrations with custom stack forms still use the fallback below.
            int separator = controllerBlockId.lastIndexOf(':');
            if (separator > controllerBlockId.indexOf(':') && separator + 1 < controllerBlockId.length()) {
                try {
                    int meta = Integer.parseInt(controllerBlockId.substring(separator + 1));
                    if (meta >= 0 && meta < GregTechAPI.METATILEENTITIES.length
                        && GregTechAPI.METATILEENTITIES[meta] instanceof IConstructable c
                        && isControllerMatch(c, controllerBlockId)) {
                        resolvedControllers.put(controllerBlockId, c);
                        return c;
                    }
                } catch (NumberFormatException ignored) {}
            }
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
        ConstructableData merged = null;
        try {
            var map = ConstructableData.getConstructableData();
            synchronized (map) {
                merged = mergeData(merged, map.get(c));
                ItemId controllerItem = resolveControllerItem(c);
                // ConstructableData is identity-keyed. StructureLib container scans may create an equivalent
                // constructable instance, so match every published entry by its controller stack as a fallback.
                // A controller can have multiple definitions (and therefore multiple channel sets); selecting
                // one entry loses channels such as glass when another definition owns them.
                for (var entry : map.object2ObjectEntrySet()) {
                    IConstructable candidate = entry.getKey();
                    if (candidate != c && controllerItem != null && isControllerMatch(candidate, controllerItem)) {
                        merged = mergeData(merged, entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {
            // The accessor is only available when BlockRenderer6343's compatibility mixin is applied.
            ConstructableData direct = ConstructableData.getTierData(c);
            merged = direct.hasData() ? direct : null;
        }
        return merged != null ? merged : ConstructableData.getTierData(c);
    }

    @Nullable
    private static ConstructableData mergeData(@Nullable ConstructableData current,
        @Nullable ConstructableData candidate) {
        if (candidate == null || !candidate.hasData()) return current;
        ConstructableData merged = current != null ? current : new ConstructableData();
        merged.setMaxTier(candidate.getMaxTotalTier(), "");
        if (candidate.getChannelMaxTierMap() != null) {
            for (var channel : candidate.getChannelMaxTierMap()
                .object2IntEntrySet()) {
                if (channel.getKey() != null && !channel.getKey()
                    .trim()
                    .isEmpty()) {
                    merged.setMaxTier(channel.getIntValue(), channel.getKey());
                }
            }
        }
        return merged;
    }

    @Nullable
    public ConstructableData getConstructableDataFor(String controllerBlockId) {
        // Before the asynchronous scans finish, a lookup may see a partial tier-only entry. Do not retain
        // that snapshot: the completed GregTech scan can add channels (notably "glass") to the same
        // controller later in the load lifecycle.
        ConstructableData cachedData = scansComplete ? resolvedData.get(controllerBlockId) : null;
        if (cachedData != null) return cachedData;
        IConstructable scanned = scannedControllers.get(controllerBlockId);
        if (scanned != null) return rememberData(controllerBlockId, getConstructableData(scanned));
        IConstructable c = findConstructable(controllerBlockId);
        if (c != null) return rememberData(controllerBlockId, getConstructableData(c));

        // Non-GregTech StructureLib controllers are not present in METATILEENTITIES; resolve them directly
        // from the published map by controller item id.
        try {
            var map = ConstructableData.getConstructableData();
            synchronized (map) {
                ConstructableData merged = null;
                IConstructable matched = null;
                for (var entry : map.object2ObjectEntrySet()) {
                    if (isControllerMatch(entry.getKey(), controllerBlockId)) {
                        if (matched == null) {
                            matched = entry.getKey();
                        }
                        merged = mergeData(merged, entry.getValue());
                    }
                }
                if (matched != null) {
                    resolvedControllers.put(controllerBlockId, matched);
                    return rememberData(controllerBlockId, merged);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private ConstructableData rememberData(String controllerBlockId, ConstructableData data) {
        if (scansComplete && data != null && data.hasData()) {
            resolvedData.putIfAbsent(controllerBlockId, data);
        }
        return data;
    }

    @Nullable
    private static ItemId resolveControllerItem(IConstructable constructable) {
        if (!(constructable instanceof IMetaTileEntity mte)) return null;
        ItemStack stack = mte.getStackForm(1);
        // Keep this identity local to one lookup. Controller registrations and scan results may still
        // change while the asynchronous scans are running; no mutable stacks are retained across builds.
        return stack != null ? ItemId.createNoCopy(stack.getItem(), stack.getItemDamage(), null) : null;
    }

    private static boolean isControllerMatch(IConstructable candidate, ItemId controllerItem) {
        if (!(candidate instanceof IMetaTileEntity mte)) return false;
        ItemStack stack = mte.getStackForm(1);
        return stack != null && stack.getItem() == controllerItem.getItem()
            && stack.getItemDamage() == controllerItem.getItemMeta();
    }
}
