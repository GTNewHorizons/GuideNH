package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public class GuidebookChunk {

    public final int chunkX;

    public final int chunkZ;

    /**
     * Preview columns are partitioned into 16-block-high sections. This keeps the vanilla
     * allocation profile for normal scenes, while allowing a level to expose arbitrary build
     * heights without allocating every intervening Y section.
     */
    private final Int2ObjectOpenHashMap<Section> sections = new Int2ObjectOpenHashMap<>();

    @Getter
    private int filledCount = 0;

    public GuidebookChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static int index(int x, int y, int z) {
        return ((x & 15) << 8) | ((z & 15) << 4) | (y & 15);
    }

    public Block getBlock(int x, int y, int z) {
        Section section = sections.get(y >> 4);
        return section != null ? section.blocks[index(x, y, z)] : null;
    }

    public int getMeta(int x, int y, int z) {
        Section section = sections.get(y >> 4);
        return section != null ? section.metas[index(x, y, z)] : 0;
    }

    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        int idx = index(x, y, z);
        int sectionY = y >> 4;
        Section section = sections.get(sectionY);
        if (section == null) {
            if (block == null || block == Blocks.air) {
                return false;
            }
            section = new Section();
            sections.put(sectionY, section);
        }
        Block prev = section.blocks[idx];
        boolean prevFilled = prev != null && prev != Blocks.air;
        boolean nextFilled = block != null && block != Blocks.air;
        section.blocks[idx] = nextFilled ? block : null;
        section.metas[idx] = meta;
        if (prevFilled && !nextFilled) {
            filledCount--;
            if (--section.filledCount == 0) {
                sections.remove(sectionY);
            }
            return true;
        } else if (!prevFilled && nextFilled) {
            filledCount++;
            section.filledCount++;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return filledCount == 0;
    }

    public void forEachBlock(BlockIterator it) {
        for (var sectionEntry : sections.int2ObjectEntrySet()) {
            int sectionY = sectionEntry.getIntKey();
            Section section = sectionEntry.getValue();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int localY = 0; localY < 16; localY++) {
                        int idx = ((lx) << 8) | ((lz) << 4) | localY;
                        Block b = section.blocks[idx];
                        if (b != null && b != Blocks.air) {
                            it.accept(lx, (sectionY << 4) + localY, lz, b, section.metas[idx]);
                        }
                    }
                }
            }
        }
    }

    private static class Section {

        private final Block[] blocks = new Block[16 * 16 * 16];
        // Keep full metadata for mods that use extended values.
        private final int[] metas = new int[16 * 16 * 16];
        private int filledCount;
    }

    @FunctionalInterface
    public interface BlockIterator {

        void accept(int localX, int y, int localZ, Block block, int meta);
    }
}
