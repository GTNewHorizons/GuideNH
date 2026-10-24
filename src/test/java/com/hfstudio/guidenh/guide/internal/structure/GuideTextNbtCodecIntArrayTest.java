package com.hfstudio.guidenh.guide.internal.structure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

class GuideTextNbtCodecIntArrayTest {

    @Test
    void intArraysOfEveryLengthSurviveAStructureRoundTrip() throws Exception {
        NBTTagCompound conduit = new NBTTagCompound();
        conduit.setIntArray("empty", new int[0]);
        conduit.setIntArray("single", new int[] { 4 });
        conduit.setIntArray("pair", new int[] { 5, 1 });
        NBTTagList conduits = new NBTTagList();
        conduits.appendTag(conduit);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setTag("conduits", conduits);

        NBTTagCompound read = readTile(GuideTextNbtCodec.writeStructureSnbt(structureWith(tile)));

        NBTTagCompound readConduit = read.getTagList("conduits", 10)
            .getCompoundTagAt(0);
        assertEquals(11, readConduit.func_150299_b("empty"));
        assertArrayEquals(new int[0], readConduit.getIntArray("empty"));
        assertArrayEquals(new int[] { 4 }, readConduit.getIntArray("single"));
        assertArrayEquals(new int[] { 5, 1 }, readConduit.getIntArray("pair"));
    }

    @Test
    void legacySingleValueIntArrayIsRestored() throws Exception {
        NBTTagCompound read = readTile("{blocks:[0:{pos:[0,0,0,],state:0,nbt:{connections:[4,],},},],}");

        assertEquals(11, read.func_150299_b("connections"));
        assertArrayEquals(new int[] { 4 }, read.getIntArray("connections"));
    }

    @Test
    void emptyListsCanBeReinterpretedAsEmptyIntArrays() throws Exception {
        NBTTagCompound legacy = (NBTTagCompound) JsonToNBT
            .func_150315_a("{conduits:[0:{conduit:{externalConnections:[],},},],}");

        NBTTagCompound repaired = GuideTextNbtCodec.withEmptyListsAsIntArrays(legacy);

        NBTTagCompound body = repaired.getTagList("conduits", 10)
            .getCompoundTagAt(0)
            .getCompoundTag("conduit");
        assertEquals(11, body.func_150299_b("externalConnections"));
        assertNull(GuideTextNbtCodec.withEmptyListsAsIntArrays(new NBTTagCompound()));
    }

    private static NBTTagCompound structureWith(NBTTagCompound tile) {
        NBTTagCompound block = new NBTTagCompound();
        block.setIntArray("pos", new int[] { 0, 0, 0 });
        block.setInteger("state", 0);
        block.setTag("nbt", tile);
        NBTTagList blocks = new NBTTagList();
        blocks.appendTag(block);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("blocks", blocks);
        return root;
    }

    private static NBTTagCompound readTile(String snbt) throws Exception {
        return GuideTextNbtCodec.readStructureNbt(snbt.getBytes(StandardCharsets.UTF_8))
            .getTagList("blocks", 10)
            .getCompoundTagAt(0)
            .getCompoundTag("nbt");
    }
}
