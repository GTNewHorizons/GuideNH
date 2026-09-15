package com.hfstudio.guidenh.guide.scene.preview;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

import blockrenderer6343.client.utils.ConstructableData;
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

    private boolean scanRequested;

    private StructureLibDefinitionCache() {}

    public static StructureLibDefinitionCache getInstance() {
        return INSTANCE;
    }

    /**
     * Starts BlockRenderer6343's scans, which it normally starts from {@code NEIConfig.loadConfig}.
     *
     * <p>
     * Two scans fill {@code ConstructableData}, each started by the static initializer of the handler that
     * owns it: the GregTech handler covers the machines in the meta tile entity registry, and the
     * StructureLib handler covers the multiblocks registered through {@code IMultiblockInfoContainer}, which
     * is how mods add multiblocks of their own. Nothing else ever starts them here, and without them no
     * controller reports tiers or channels, so both sliders stay hidden for the whole session.
     *
     * <p>
     * Called once when loading completes, the first moment every mod has registered its multiblocks. Only the
     * class is initialized rather than an instance built, because constructing a handler would also register
     * its recipe handler with NotEnoughItems a second time. Both scans publish on their own thread and each
     * publishes separately, so a reader must tolerate the map growing; {@code ConstructableData.getTierData}
     * reads the live map and does exactly that.
     */
    public void startScans() {
        if (scanRequested || !Mods.BlockRenderer6343.isModLoaded()) {
            return;
        }
        scanRequested = true;
        // Both handlers extend NotEnoughItems' recipe handler and are only reached from the client-side scene,
        // so a missing class is reported instead of being allowed to abort the caller.
        initializeScan("blockrenderer6343.integration.structurelib.StructureCompatNEIHandler");
        if (Mods.GregTech.isModLoaded()) {
            initializeScan("blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler");
        }
    }

    private static void initializeScan(String className) {
        try {
            Class.forName(className, true, StructureLibDefinitionCache.class.getClassLoader());
        } catch (Throwable t) {
            GuideDebugLog
                .warnAlways("[GuideNH] [StructureLib] Could not start the scan in {}: {}", className, t.toString());
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
     * The tier and channel ranges of a machine, or an empty default when it exposes none.
     */
    public ConstructableData getConstructableData(IConstructable c) {
        return ConstructableData.getTierData(c);
    }

    @Nullable
    public ConstructableData getConstructableDataFor(String controllerBlockId) {
        IConstructable c = findConstructable(controllerBlockId);
        return c != null ? getConstructableData(c) : null;
    }
}
