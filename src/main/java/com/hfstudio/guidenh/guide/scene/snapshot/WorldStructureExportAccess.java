package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class WorldStructureExportAccess implements StructureExportAccess {

    private final World world;
    private final boolean requireLoadedChunks;

    public WorldStructureExportAccess(World world) {
        this(world, false);
    }

    public WorldStructureExportAccess(World world, boolean requireLoadedChunks) {
        this.world = world;
        this.requireLoadedChunks = requireLoadedChunks;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        requireLoaded(x, y, z);
        return world.getBlock(x, y, z);
    }

    private void requireLoaded(int x, int y, int z) {
        if (requireLoadedChunks && !world.blockExists(x, y, z)) {
            throw new IllegalArgumentException("The selected region contains unloaded chunks");
        }
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        requireLoaded(x, y, z);
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        requireLoaded(x, y, z);
        return world.getTileEntity(x, y, z);
    }

    @Override
    public String getBlockId(int x, int y, int z, Block block) {
        return Block.blockRegistry.getNameForObject(block);
    }

    @Override
    public World getSourceWorld() {
        return world;
    }
}
