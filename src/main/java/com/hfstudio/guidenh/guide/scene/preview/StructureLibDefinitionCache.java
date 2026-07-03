package com.hfstudio.guidenh.guide.scene.preview;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.client.utils.ConstructableData;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * Full resident cache holding IConstructable definitions and tier/channel metadata.
 * Does NOT cache construct() output. No LRU eviction. No serialization.
 *
 * Refreshed on reload via {@link #refresh()} from BlockRenderer6343 runtime data.
 */
public class StructureLibDefinitionCache {

    private static final StructureLibDefinitionCache INSTANCE = new StructureLibDefinitionCache();

    /** IConstructable → tier/channel metadata. Full population, no eviction. */
    private volatile Map<IConstructable, ConstructableData> constructableDataMap = Collections.emptyMap();

    /** All known multiblock definitions. */
    private volatile List<IConstructable> multiblocksList = Collections.emptyList();

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
    }

    // ===== Lifecycle =====

    /**
     * Sync full data from BlockRenderer6343 runtime.
     * If BlockRenderer6343 is not loaded, maps remain empty.
     */
    @SuppressWarnings("unchecked")
    public void refresh() {
        try {
            Object2ObjectMap<IConstructable, ConstructableData> dataMap = com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343.AccessorConstructableData
                .getConstructableDataMap();
            constructableDataMap = dataMap != null ? Collections.unmodifiableMap(dataMap) : Collections.emptyMap();

            List<IConstructable> list = com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343.AccessorGTNEIMultiblockHandler
                .getMultiblocksList();
            multiblocksList = list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
        } catch (Throwable t) {
            constructableDataMap = Collections.emptyMap();
            multiblocksList = Collections.emptyList();
        }
    }

    public boolean isAvailable() {
        return !constructableDataMap.isEmpty();
    }

    // ===== IConstructable queries =====

    /**
     * Find IConstructable by controller blockId string (e.g. "gregtech:gt.blockmachines:123").
     * Iterates multiblocksList, matching IMetaTileEntity.getStackForm(1).
     *
     * @return matching IConstructable, or null if not found
     */
    @Nullable
    public IConstructable findConstructable(String controllerBlockId) {
        if (controllerBlockId == null || controllerBlockId.isEmpty()) return null;
        for (IConstructable c : multiblocksList) {
            if (isControllerMatch(c, controllerBlockId)) return c;
        }
        return null;
    }

    private static boolean isControllerMatch(IConstructable c, String blockId) {
        if (c instanceof gregtech.api.interfaces.metatileentity.IMetaTileEntity mte) {
            net.minecraft.item.ItemStack stack = mte.getStackForm(1);
            if (stack == null || stack.getItem() == null) return false;
            String id = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
            if (id == null) return false;
            String full = id + ":" + stack.getItemDamage();
            return full.equals(blockId) || id.equals(blockId);
        }
        return false;
    }

    @Nullable
    public ConstructableData getConstructableData(IConstructable c) {
        ConstructableData data = constructableDataMap.get(c);
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

    public List<IConstructable> getMultiblocksList() {
        return multiblocksList;
    }
}
