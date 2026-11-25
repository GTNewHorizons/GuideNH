package com.hfstudio.guidenh.guide.internal.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import com.hfstudio.guidenh.guide.internal.resource.GuideBoundedInputStream;

public class GuideBinaryNbtReader {

    private final DataInputStream input;
    private final GuideNbtReadLimits limits;
    private long remainingAllocationBytes;
    private int remainingTags;

    public GuideBinaryNbtReader(InputStream input, GuideNbtReadLimits limits) {
        this.input = new DataInputStream(new GuideBoundedInputStream(input, limits.maxBytes()));
        this.limits = limits;
        remainingAllocationBytes = limits.maxAllocationBytes();
        remainingTags = limits.maxTags();
    }

    public NBTTagCompound readCompound() throws IOException {
        if (input.readUnsignedByte() != 10) throw new IOException("Binary NBT root must be a compound");
        readString();
        return (NBTTagCompound) readTag(10, 0);
    }

    private NBTBase readTag(int type, int depth) throws IOException {
        if (depth > limits.maxDepth() || remainingTags-- <= 0) {
            throw new IOException("Binary NBT exceeds its structural limits");
        }
        reserve(64L);
        return switch (type) {
            case 1 -> new NBTTagByte(input.readByte());
            case 2 -> new NBTTagShort(input.readShort());
            case 3 -> new NBTTagInt(input.readInt());
            case 4 -> new NBTTagLong(input.readLong());
            case 5 -> new NBTTagFloat(input.readFloat());
            case 6 -> new NBTTagDouble(input.readDouble());
            case 7 -> readByteArray();
            case 8 -> new NBTTagString(readString());
            case 9 -> readList(depth);
            case 10 -> readCompoundBody(depth);
            case 11 -> readIntArray();
            default -> throw new IOException("Unknown binary NBT tag type: " + type);
        };
    }

    private NBTTagByteArray readByteArray() throws IOException {
        int count = readArrayLength(1);
        byte[] bytes = new byte[count];
        input.readFully(bytes);
        return new NBTTagByteArray(bytes);
    }

    private NBTTagIntArray readIntArray() throws IOException {
        int count = readArrayLength(4);
        int[] values = new int[count];
        for (int index = 0; index < count; index++) values[index] = input.readInt();
        return new NBTTagIntArray(values);
    }

    private int readArrayLength(int elementBytes) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > limits.maxBytes() / elementBytes) {
            throw new IOException("Binary NBT array exceeds its byte limit");
        }
        reserve((long) count * elementBytes);
        return count;
    }

    private NBTTagList readList(int depth) throws IOException {
        int type = input.readUnsignedByte();
        int count = input.readInt();
        if (count < 0 || count > remainingTags || type > 11 || (type == 0 && count != 0)) {
            throw new IOException("Invalid or oversized binary NBT list");
        }
        reserve((long) count * 8L);
        NBTTagList list = new NBTTagList();
        if (count == 0 && type != 0) {
            list.appendTag(emptyTag(type));
            list.removeTag(0);
        }
        for (int index = 0; index < count; index++) list.appendTag(readTag(type, depth + 1));
        return list;
    }

    private static NBTBase emptyTag(int type) throws IOException {
        return switch (type) {
            case 1 -> new NBTTagByte((byte) 0);
            case 2 -> new NBTTagShort((short) 0);
            case 3 -> new NBTTagInt(0);
            case 4 -> new NBTTagLong(0L);
            case 5 -> new NBTTagFloat(0F);
            case 6 -> new NBTTagDouble(0D);
            case 7 -> new NBTTagByteArray(new byte[0]);
            case 8 -> new NBTTagString("");
            case 9 -> new NBTTagList();
            case 10 -> new NBTTagCompound();
            case 11 -> new NBTTagIntArray(new int[0]);
            default -> throw new IOException("Unknown binary NBT tag type: " + type);
        };
    }

    private NBTTagCompound readCompoundBody(int depth) throws IOException {
        NBTTagCompound compound = new NBTTagCompound();
        int type;
        while ((type = input.readUnsignedByte()) != 0) {
            String name = readString();
            reserve(48L);
            compound.setTag(name, readTag(type, depth + 1));
        }
        return compound;
    }

    private String readString() throws IOException {
        String value = input.readUTF();
        reserve(40L + (long) value.length() * 2L);
        return value;
    }

    private void reserve(long bytes) throws IOException {
        if (bytes > remainingAllocationBytes) throw new IOException("Binary NBT exceeds its allocation limit");
        remainingAllocationBytes -= bytes;
    }
}
