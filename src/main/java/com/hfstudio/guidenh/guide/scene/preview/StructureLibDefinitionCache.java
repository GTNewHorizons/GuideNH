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
     * Starts BlockRenderer6343's structure scans if Nothing has published them yet.
     *
     * <p>
     * BlockRenderer6343 fills {@code ConstructableData} from two scans, both started by the static initializer
     * of the class that owns them: its GregTech handler covers the machines in the meta tile entity registry,
     * and its StructureLib handler covers every multiblock registered through {@code IMultiblockInfoContainer},
     * which includes the mods that build their multiblocks on top of GregTech. Those initializers normally run
     * from {@code NEIConfig.loadConfig}, and when that does not happen the map stays empty for the whole
     * session, so nothing reports tiers or channels and both sliders stay hidden.
     *
     * <p>
     * Only the class is initialized, not an instance built, because constructing a handler would also register
     * a recipe handler with NotEnoughItems a second time. Initialization is attempted once; the scans it starts
     * publish asynchronously, so an empty map right afterwards is expected and {@link #dataMap()} keeps
     * re-reading until they do.
     */
    private void requestScan() {
        if (scanRequested || !Mods.BlockRenderer6343.isModLoaded()) {
            return;
        }
        scanRequested = true;
        // Both handlers only make sense on the client: they extend NotEnoughItems' recipe handler and are
        // reached from the guide's client-side scene, so a missing client class is reported rather than
        // allowed to abort the caller.
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
     * The snapshot, re-read while it is still empty because BlockRenderer6343 publishes asynchronously.
     *
     * <p>
     * A scene materializes long before the scan finishes, so the first lookups see nothing. Asking for the
     * scan and reading again keeps those lookups correct once it has published, and because a snapshot with
     * entries is never re-read, a populated session costs one lookup per query.
     */
    private Map<IConstructable, ConstructableData> dataMap() {
        Map<IConstructable, ConstructableData> current = constructableDataMap;
        if (!current.isEmpty()) {
            return current;
        }
        requestScan();
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
