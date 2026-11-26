package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class GuideNbtMemoryUsage {

    private GuideNbtMemoryUsage() {}

    public static long estimate(NBTBase tag) {
        return estimate(tag, 0);
    }

    private static long estimate(NBTBase tag, int depth) {
        if (depth > GuideNbtReadLimits.DEFAULT.maxDepth()) {
            throw new IllegalArgumentException("NBT exceeds its nesting limit");
        }
        long bytes = 64L;
        if (tag instanceof NBTTagCompound compound) {
            for (String key : compound.func_150296_c()) {
                bytes += 48L + (long) key.length() * 2L + estimate(compound.getTag(key), depth + 1);
            }
        } else if (tag instanceof NBTTagList list) {
            for (Object child : list.tagList) bytes += 8L + estimate((NBTBase) child, depth + 1);
        } else if (tag instanceof NBTTagByteArray array) {
            bytes += array.func_150292_c().length;
        } else if (tag instanceof NBTTagIntArray array) {
            bytes += (long) array.func_150302_c().length * 4L;
        } else if (tag instanceof NBTTagString text) {
            bytes += (long) text.func_150285_a_()
                .length() * 2L;
        }
        return bytes;
    }
}
