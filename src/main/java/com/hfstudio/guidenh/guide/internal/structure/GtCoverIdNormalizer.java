package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Rewrites GregTech cover item ids between packed numeric ids and registry-name strings.
 * Only {@code gt.covers[*].id} is transformed.
 */
public class GtCoverIdNormalizer {

    public static final String PALETTE_TAG = "palette";
    public static final String STATE_TAG = "state";
    public static final String NAME_TAG = "Name";
    public static final String GT_COVERS_TAG = "gt.covers";
    public static final String ID_TAG = "id";
    public static final String GT_BLOCK_MACHINES = "gregtech:gt.blockmachines";
    public static final String GT_BLOCK_FRAMES = "gregtech:gt.blockframes";
    public static final String BASE_META_TILE_ENTITY_ID = "BaseMetaTileEntity";
    public static final String BASE_META_PIPE_ENTITY_ID = "BaseMetaPipeEntity";

    private GtCoverIdNormalizer() {}

    public static boolean[] resolveCoverCapableStates(NBTTagCompound root) {
        if (root == null || !root.hasKey(PALETTE_TAG, 9)) {
            return new boolean[0];
        }
        return resolveCoverCapableStates(root.getTagList(PALETTE_TAG, 10));
    }

    public static boolean[] resolveCoverCapableStates(NBTTagList palette) {
        boolean[] coverCapableStates = new boolean[palette.tagCount()];
        boolean found = false;

        for (int index = 0; index < palette.tagCount(); index++) {
            NBTTagCompound paletteEntry = palette.getCompoundTagAt(index);
            if (!paletteEntry.hasKey(NAME_TAG, 8)) {
                continue;
            }
            String blockName = paletteEntry.getString(NAME_TAG);
            if (!GT_BLOCK_MACHINES.equals(blockName) && !GT_BLOCK_FRAMES.equals(blockName)) {
                continue;
            }
            coverCapableStates[index] = true;
            found = true;
        }

        return found ? coverCapableStates : new boolean[0];
    }

    public static void rewriteBlockTileTag(NBTTagCompound blockTag, NBTTagCompound tileTag, boolean[] coverCapableStates,
        boolean encode) {
        if (coverCapableStates.length == 0 || !blockTag.hasKey(STATE_TAG, 99)) {
            return;
        }

        int state = blockTag.getInteger(STATE_TAG);
        if (state < 0 || state >= coverCapableStates.length || !coverCapableStates[state]) {
            return;
        }

        if (!isGtCoverableTile(tileTag) || !tileTag.hasKey(GT_COVERS_TAG, 9)) {
            return;
        }

        rewriteCoverList(tileTag.getTagList(GT_COVERS_TAG, 10), encode);
    }

    public static boolean isGtCoverableTile(NBTTagCompound tileTag) {
        if (!tileTag.hasKey(ID_TAG, 8)) {
            return false;
        }

        String tileEntityId = tileTag.getString(ID_TAG);
        return BASE_META_TILE_ENTITY_ID.equals(tileEntityId) || BASE_META_PIPE_ENTITY_ID.equals(tileEntityId);
    }

    private static void rewriteCoverList(NBTTagList covers, boolean encode) {
        for (int index = 0; index < covers.tagCount(); index++) {
            NBTTagCompound cover = covers.getCompoundTagAt(index);
            if (encode) {
                encodeCoverId(cover);
            } else {
                decodeCoverId(cover);
            }
        }
    }

    private static void encodeCoverId(NBTTagCompound cover) {
        if (!cover.hasKey(ID_TAG, 99)) {
            return;
        }

        int packedId = cover.getInteger(ID_TAG);
        int itemId = packedId & 0xFFFF;
        int meta = packedId >>> 16;
        if (itemId == 0) {
            return;
        }

        Item item = Item.getItemById(itemId);
        if (item == null) {
            return;
        }

        Object rawRegistryName = Item.itemRegistry.getNameForObject(item);
        if (rawRegistryName == null) {
            return;
        }

        String registryName = rawRegistryName.toString();
        if (registryName.isEmpty()) {
            return;
        }

        cover.setString(ID_TAG, registryName + ":" + meta);
    }

    private static void decodeCoverId(NBTTagCompound cover) {
        if (!cover.hasKey(ID_TAG, 8)) {
            return;
        }

        String encodedId = cover.getString(ID_TAG);
        if (encodedId == null || encodedId.isEmpty()) {
            return;
        }

        int separatorIndex = encodedId.lastIndexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= encodedId.length() - 1) {
            return;
        }

        String registryName = encodedId.substring(0, separatorIndex);
        String metaText = encodedId.substring(separatorIndex + 1);
        int meta;
        try {
            meta = Integer.parseInt(metaText);
        } catch (NumberFormatException ignored) {
            return;
        }

        if (meta < 0 || meta > 0xFFFF) {
            return;
        }

        Item item = (Item) Item.itemRegistry.getObject(registryName);
        if (item == null) {
            return;
        }

        int itemId = Item.getIdFromItem(item);
        if (itemId <= 0 || itemId > 0xFFFF) {
            return;
        }

        cover.setInteger(ID_TAG, itemId | (meta << 16));
    }
}
