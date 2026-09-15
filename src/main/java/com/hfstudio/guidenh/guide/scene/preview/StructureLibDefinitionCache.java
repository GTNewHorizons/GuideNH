package com.hfstudio.guidenh.guide.scene.preview;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
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

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
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
     * The snapshot, re-read while it is still empty because BlockRenderer6343 fills its map asynchronously.
     *
     * <p>
     * That scan starts when NotEnoughItems loads its multiblock handler, which is after the resource reload
     * that calls {@link #refresh()}, and the accessor mixin is applied later still. A snapshot taken at
     * reload time therefore fails and stays empty for the whole session, which would report that no
     * controller has tiers or channels and hide both sliders. Retrying while empty covers that ordering, and
     * once a snapshot has data it is never re-read per lookup.
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
