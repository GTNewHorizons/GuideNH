package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookFakeChunk extends Chunk {

    public GuidebookLevel level;

    public GuidebookFakeChunk(GuidebookFakeWorld world, int chunkX, int chunkZ) {
        super(world, chunkX, chunkZ);
        this.level = world.getGuidebookLevel();
        this.isChunkLoaded = true;
        this.isTerrainPopulated = true;
        this.isLightPopulated = true;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return level.getBlock(worldX(x), y, worldZ(z));
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return level.getBlockMetadata(worldX(x), y, worldZ(z));
    }

    @Override
    public TileEntity getTileEntityUnsafe(int x, int y, int z) {
        return getLevelTileEntity(x, y, z);
    }

    @Override
    public TileEntity func_150806_e(int x, int y, int z) {
        return getLevelTileEntity(x, y, z);
    }

    @Override
    public void addTileEntity(TileEntity tileEntity) {
        if (tileEntity != null) {
            worldObj.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
        }
    }

    @Override
    public void func_150812_a(int x, int y, int z, TileEntity tileEntity) {
        worldObj.setTileEntity(worldX(x), y, worldZ(z), tileEntity);
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        worldObj.removeTileEntity(worldX(x), y, worldZ(z));
    }

    @Override
    public void removeInvalidTileEntity(int x, int y, int z) {}

    @Override
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        return 15;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z, int skylightSubtracted) {
        return 15;
    }

    @Override
    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return true;
    }

    private TileEntity getLevelTileEntity(int x, int y, int z) {
        TileEntity tileEntity = level.getTileEntity(worldX(x), y, worldZ(z));
        return tileEntity == null || tileEntity.isInvalid() ? null : tileEntity;
    }

    private int worldX(int x) {
        return xPosition * 16 + x;
    }

    private int worldZ(int z) {
        return zPosition * 16 + z;
    }
}
