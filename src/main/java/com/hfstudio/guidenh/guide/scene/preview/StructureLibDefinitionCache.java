package com.hfstudio.guidenh.guide.scene.preview;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343.AccessorConstructableData;

import blockrenderer6343.client.utils.ConstructableData;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * Tier/channel metadata cache from BlockRenderer6343.
 * Only determines whether UI sliders appear. Does NOT determine whether a machine can be rendered.
 * <p>
 * Machine discovery is done via {@code GregTechAPI.METATILEENTITIES} — the full unfiltered list.
 * {@code ConstructableData} provides tier/channel metadata for the subset of machines that have them.
 */
public class StructureLibDefinitionCache {

    private static final StructureLibDefinitionCache INSTANCE = new StructureLibDefinitionCache();

    private volatile Map<IConstructable, ConstructableData> constructableDataMap = Collections.emptyMap();
    private boolean scanRequested;

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
    }

    /**
     * Starts BlockRenderer6343's structure scans, which it normally starts from {@code NEIConfig.loadConfig}.
     *
     * <p>
     * Both scans fill the map this cache reads, and each is started by the static initializer of the handler
     * that owns it: the GregTech handler covers the machines in the meta tile entity registry, and the
     * StructureLib handler covers the multiblocks registered through {@code IMultiblockInfoContainer}. When
     * that initializer never runs, the map stays empty for the whole session, so no controller reports tiers
     * or channels and both sliders stay hidden.
     *
     * <p>
     * Called once when loading completes, which is the first moment every mod has registered its multiblocks.
     * Only the class is initialized rather than an instance built, because constructing a handler would also
     * register its recipe handler with NotEnoughItems a second time. The scans then publish on their own
     * thread, so callers still re-read through {@link #dataMap()}.
     */
    public void startScans() {
        if (scanRequested || !Mods.BlockRenderer6343.isModLoaded()) {
            return;
        }
        scanRequested = true;
        // Both handlers extend NotEnoughItems' recipe handler and are only reached from the client-side scene,
        // so a missing client class is reported instead of being allowed to abort the caller.
        initializeScan("blockrenderer6343.integration.structurelib.StructureCompatNEIHandler");
        if (Mods.GregTech.isModLoaded()) {
            initializeScan("blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler");
        }
    }

    private static void initializeScan(String className) {
        try {
            Class.forName(className, true, StructureLibDefinitionCache.class.getClassLoader());
        } catch (Throwable t) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [StructureLib] Could not start the BlockRenderer6343 scan in {}: {}",
                className,
                t.toString());
        }
    }

    /**
     * Reads BlockRenderer6343's map into a snapshot.
     *
     * <p>
     * BlockRenderer6343 publishes it from its own scan thread while holding the map as the lock, so the copy
     * is taken under the same lock to avoid reading a map mid-resize. Every failure mode ends in an empty
     * snapshot because the caller cannot tell a missing accessor from a genuinely empty map.
     */
    @SuppressWarnings("unchecked")
    public void refresh() {
        try {
            Object2ObjectMap<IConstructable, ConstructableData> dataMap = AccessorConstructableData
                .getConstructableDataMap();
            if (dataMap == null) {
                constructableDataMap = Collections.emptyMap();
                return;
            }
            synchronized (dataMap) {
                constructableDataMap = Collections.unmodifiableMap(new HashMap<>(dataMap));
            }
            GuideDebugLog.warnAlways(
                "[GuideNH] [StructureLib] Definition cache refreshed: entries={}",
                constructableDataMap.size());
        } catch (Throwable t) {
            GuideDebugLog.warnAlways("[GuideNH] [StructureLib] Definition cache refresh failed: {}", t.toString());
            constructableDataMap = Collections.emptyMap();
        }
    }

    /**
     * The current snapshot, re-read while it is still empty.
     *
     * <p>
     * The scans publish on their own thread after {@link #startScans()}, so a scene that opens immediately
     * afterwards can still find nothing. Re-reading until the first entries appear covers that window, and
     * once a snapshot has entries it is never re-read, so a populated session reads a field per lookup.
     */
    private Map<IConstructable, ConstructableData> dataMap() {
        Map<IConstructable, ConstructableData> current = constructableDataMap;
        if (!current.isEmpty()) {
            return current;
        }
        refresh();
        return constructableDataMap;
    }

    /**
     * Find IConstructable by controller blockId (e.g. "gregtech:gt.blockmachines:3013").
     * Iterates GregTechAPI.METATILEENTITIES — the full unfiltered list of all GT machines.
     * Does NOT depend on ConstructableData (which only covers machines with tiered elements).
     */
    @Nullable
    public IConstructable findConstructable(String controllerBlockId) {
        if (controllerBlockId == null || controllerBlockId.isEmpty()) return null;
        try {
            for (IMetaTileEntity mte : GregTechAPI.METATILEENTITIES) {
                if (mte instanceof IConstructable c && isControllerMatch(c, controllerBlockId)) {
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
     * Get tier/channel metadata for a machine. If the machine has no tiered elements,
     * falls back to ConstructableData.getTierData() which returns an empty default (maxTotalTier=1).
     * Only determines whether tier/channel sliders appear — does NOT affect rendering.
     */
    public ConstructableData getConstructableData(IConstructable c) {
        ConstructableData data = dataMap().get(c);
        return data != null ? data : ConstructableData.getTierData(c);
    }

    @Nullable
    public ConstructableData getConstructableDataFor(String controllerBlockId) {
        IConstructable c = findConstructable(controllerBlockId);
        return c != null ? getConstructableData(c) : null;
    }

    public Map<IConstructable, ConstructableData> getAllConstructableData() {
        return constructableDataMap;
    }
}
