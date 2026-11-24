package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class GuideStructureSchema {

    private GuideStructureSchema() {}

    public static void validate(NBTTagCompound root) {
        if (!root.hasKey("size", 11)) {
            throw new IllegalArgumentException("Structure size must be an integer array");
        }
        int[] size = root.getIntArray("size");
        if (size.length != 3 || size[0] <= 0 || size[1] <= 0 || size[2] <= 0) {
            throw new IllegalArgumentException("Structure size must contain three positive dimensions");
        }
        NBTTagList palette = requireCompoundList(root, "palette");
        NBTTagList blocks = requireCompoundList(root, "blocks");
        for (int index = 0; index < palette.tagCount(); index++) {
            NBTTagCompound entry = palette.getCompoundTagAt(index);
            if (!entry.hasKey("Name", 8) || entry.getString("Name")
                .trim()
                .isEmpty()) {
                throw new IllegalArgumentException("Structure palette entries must name a block");
            }
        }
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound entry = blocks.getCompoundTagAt(index);
            int state = entry.getInteger("state");
            if (!entry.hasKey("state", 3) || state < 0 || state >= palette.tagCount()) {
                throw new IllegalArgumentException("Structure block state is outside the palette");
            }
            int[] position = entry.getIntArray("pos");
            if (position.length != 3) {
                throw new IllegalArgumentException("Structure block position must contain three coordinates");
            }
            for (int axis = 0; axis < 3; axis++) {
                if (position[axis] < 0 || position[axis] >= size[axis]) {
                    throw new IllegalArgumentException("Structure block position is outside its dimensions");
                }
            }
        }
    }

    private static NBTTagList requireCompoundList(NBTTagCompound root, String key) {
        if (!(root.getTag(key) instanceof NBTTagList list) || (list.tagCount() > 0 && list.func_150303_d() != 10)) {
            throw new IllegalArgumentException("Structure " + key + " must be a list of compounds");
        }
        return list;
    }
}
