package com.hfstudio.guidenh.guide.scene.level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.scene.GuidebookFakeRenderEnvironment;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Lightweight client-only world wrapper backed by a {@link GuidebookLevel}.
 */
@SideOnly(Side.CLIENT)
public class GuidebookFakeWorld extends WorldClient implements GuidebookPreviewWorld {

    public static final long FROZEN_WORLD_TIME = 0L;
    public static volatile boolean gregTechDummyWorldRegistrationAttempted;

    private final GuidebookLevel level;
    /** Stable read-only chunk views; renderer/CTM asks for the same coordinates repeatedly. */
    private final Long2ObjectOpenHashMap<GuidebookFakeChunk> chunkViews = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<TileEntity> trackedTileEntities = new Long2ObjectOpenHashMap<>();
    @Nullable
    private Set<TileEntity> markBlockForUpdateGuard;
    private final Set<Long> neighborUpdateGuard = new HashSet<>();

    public GuidebookFakeWorld(GuidebookLevel level) {
        super(
            resolveNetHandler(),
            new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT),
            resolveDimensionId(),
            resolveDifficulty(),
            new Profiler());
        this.level = level;
        this.isRemote = true;
        registerOptionalDummyWorldIntegrations();
    }

    public static NetHandlerPlayClient resolveNetHandler() {
        var netHandler = Minecraft.getMinecraft()
            .getNetHandler();
        if (netHandler == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }
        return netHandler;
    }

    public static int resolveDimensionId() {
        var currentWorld = Minecraft.getMinecraft().theWorld;
        return currentWorld != null ? currentWorld.provider.dimensionId : 0;
    }

    public static EnumDifficulty resolveDifficulty() {
        var currentWorld = Minecraft.getMinecraft().theWorld;
        return currentWorld != null ? currentWorld.difficultySetting : EnumDifficulty.NORMAL;
    }

    public static void registerOptionalDummyWorldIntegrations() {
        if (gregTechDummyWorldRegistrationAttempted) {
            return;
        }
        gregTechDummyWorldRegistrationAttempted = true;
        GuideNhIntegrationRegistry.global()
            .registerDummyWorldIntegrations(GuidebookFakeWorld.class);
    }

    public GuidebookLevel getGuidebookLevel() {
        return level;
    }

    @Override
    public void closePreviewWorld() {
        GuidebookPreviewBlockPlacer.releasePlacementEntity(this);
        // WorldClient owns mutable entity/tile lists in addition to the level's canonical maps.
        // Clear those lists before dropping renderer references so a stale world cannot retain
        // scene objects after the page that created it is evicted.
        loadedTileEntityList.clear();
        loadedEntityList.clear();
        playerEntities.clear();
        weatherEffects.clear();
        chunkViews.clear();
        trackedTileEntities.clear();
        markBlockForUpdateGuard = null;
        neighborUpdateGuard.clear();
        GuidebookFakeRenderEnvironment.releaseCachedPreviewPlayer(this);
        GuidebookLevelRenderer.getInstance()
            .releaseLevel(level);
    }

    @Override
    public Entity getEntityByID(int id) {
        if (level == null) {
            return null;
        }
        return level.getEntity(id);
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (level == null) return Blocks.air;
        return level.getBlock(x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (level == null) return 0;
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        Integer bartWorksMeta = GregTechHelpers.resolveBartWorksMetadata(tileEntity);
        if (bartWorksMeta != null) {
            return bartWorksMeta;
        }
        return level.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        if (level == null) return null;
        return level.getTileEntity(x, y, z);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == Blocks.air;
    }

    @Override
    public Chunk getChunkFromBlockCoords(int x, int z) {
        return getChunkFromChunkCoords(x >> 4, z >> 4);
    }

    @Override
    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        if (level == null) {
            // Still inside the World constructor; the views need the level.
            return super.getChunkFromChunkCoords(chunkX, chunkZ);
        }
        long key = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        GuidebookFakeChunk chunk = chunkViews.get(key);
        if (chunk == null) {
            chunk = new GuidebookFakeChunk(this, chunkX, chunkZ);
            chunkViews.put(key, chunk);
        }
        return chunk;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        return 15;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return (15 << 20) | (15 << 4);
    }

    @Override
    public int getBlockLightValue_do(int x, int y, int z, boolean p_72849_4_) {
        return 15;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) {
        if (level == null) {
            return 0;
        }
        return level.isBlockProvidingPowerTo(x, y, z, directionIn);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return level != null ? level.getMaxBuildHeightExclusive() : GuidebookLevel.DEFAULT_MAX_BUILD_HEIGHT_EXCLUSIVE;
    }

    @Override
    public void tick() {}

    @Override
    public boolean blockExists(int x, int y, int z) {
        return level != null && level.isValidBuildHeight(y);
    }

    @Override
    public boolean checkChunksExist(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true;
    }

    @Override
    public boolean doChunksNearChunkExist(int x, int y, int z, int radius) {
        return true;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public long getTotalWorldTime() {
        return FROZEN_WORLD_TIME;
    }

    @Override
    public long getWorldTime() {
        return FROZEN_WORLD_TIME;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        if (level == null) return _default;
        return level.isSideSolid(x, y, z, side, _default);
    }

    @Override
    public boolean func_147451_t(int x, int y, int z) {
        return false;
    }

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        TileEntity tileEntity = getTileEntity(x, y, z);
        if (tileEntity == null) {
            return;
        }
        // Optional integrations may suppress stale tile description refreshes.
        if (suppressAe2StaleTileDescriptionRefresh(tileEntity)) {
            return;
        }
        Set<TileEntity> inProgress = getOrCreateMarkBlockForUpdateGuard();
        if (!inProgress.add(tileEntity)) {
            return;
        }
        try {
            applyDescriptionPacketToTileEntity(tileEntity);
        } finally {
            inProgress.remove(tileEntity);
        }
    }

    @Override
    public void markTileEntityChunkModified(int x, int y, int z, TileEntity tileEntity) {}

    @Override
    public void addBlockEvent(int x, int y, int z, Block block, int eventId, int eventParameter) {
        Block actualBlock = getBlock(x, y, z);
        if (actualBlock == null || actualBlock == Blocks.air) {
            actualBlock = block;
        }
        if (actualBlock == null || actualBlock == Blocks.air) {
            return;
        }

        boolean handled = false;
        try {
            handled = actualBlock.onBlockEventReceived(this, x, y, z, eventId, eventParameter);
        } catch (Throwable failure) {
            GuideDebugLog.warn(
                "Preview block event failed at ({}, {}, {}) for {} (id={}, value={})",
                x,
                y,
                z,
                actualBlock,
                eventId,
                eventParameter,
                failure);
        }
        if (handled) {
            return;
        }

        TileEntity tileEntity = getTileEntity(x, y, z);
        if (tileEntity == null || tileEntity.isInvalid()) {
            return;
        }
        try {
            tileEntity.receiveClientEvent(eventId, eventParameter);
        } catch (Throwable failure) {
            GuideDebugLog.warn(
                "Preview TileEntity event failed at ({}, {}, {}) for {} (id={}, value={})",
                x,
                y,
                z,
                tileEntity.getClass()
                    .getName(),
                eventId,
                eventParameter,
                failure);
        }
    }

    @Override
    public void notifyBlockChange(int x, int y, int z, Block block) {
        notifyBlocksOfNeighborChange(x, y, z, block != null ? block : getBlock(x, y, z));
    }

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {
        long sourceKey = GuidebookLevel.packPos(x, y, z);
        if (!neighborUpdateGuard.add(sourceKey)) {
            return;
        }
        try {
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                int neighborX = x + direction.offsetX;
                int neighborY = y + direction.offsetY;
                int neighborZ = z + direction.offsetZ;
                Block neighbor = getBlock(neighborX, neighborY, neighborZ);
                if (neighbor == null || neighbor == Blocks.air) {
                    continue;
                }
                try {
                    neighbor.onNeighborBlockChange(this, neighborX, neighborY, neighborZ, block);
                } catch (Throwable failure) {
                    GuideDebugLog.warn(
                        "Preview neighbour update failed at ({}, {}, {}) for {} caused by ({}, {}, {})",
                        neighborX,
                        neighborY,
                        neighborZ,
                        neighbor,
                        x,
                        y,
                        z,
                        failure);
                }
            }
        } finally {
            neighborUpdateGuard.remove(sourceKey);
        }
    }

    public void refreshPreviewNeighbors(Collection<int[]> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (int[] position : positions) {
            if (position == null || position.length < 3) {
                continue;
            }
            int x = position[0];
            int y = position[1];
            int z = position[2];
            Block block = getBlock(x, y, z);
            if (block != null && block != Blocks.air) {
                notifyBlockChange(x, y, z, block);
            }
        }
    }

    public void refreshPreviewTileEntityNeighbors(Iterable<TileEntity> tileEntities) {
        if (tileEntities == null) {
            return;
        }
        for (TileEntity tileEntity : tileEntities) {
            if (tileEntity == null || tileEntity.isInvalid()) {
                continue;
            }
            notifyBlockChange(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity.getBlockType());
        }
    }

    public void refreshTileEntityDescriptions(Collection<TileEntity> tileEntities) {
        if (tileEntities == null || tileEntities.isEmpty()) {
            return;
        }
        for (TileEntity tileEntity : tileEntities) {
            // GregTech's description rebuilds covers without their data; its prepare step replays the meta tile
            // stream instead.
            if (tileEntity == null || tileEntity.isInvalid() || GregTechHelpers.isGregTechTileEntity(tileEntity)) {
                continue;
            }
            markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
        }
    }

    public void syncTileEntityAt(int x, int y, int z) {
        long key = GuidebookLevel.packPos(x, y, z);
        TileEntity current = level.getTileEntity(x, y, z);
        TileEntity previous = trackedTileEntities.get(key);
        if (previous == current) {
            return;
        }
        if (previous != null) {
            unregisterTrackedTileEntity(x, y, z, previous);
        }
        trackedTileEntities.remove(key);
        if (current != null && !current.isInvalid()) {
            current.setWorldObj(this);
            current.validate();
            addTileEntity(current);
            trackedTileEntities.put(key, current);
        }
    }

    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tileEntityIn) {
        unregisterTrackedTileEntity(x, y, z, null);
        level.setTileEntity(x, y, z, tileEntityIn);
        if (tileEntityIn != null) {
            tileEntityIn.validate();
            addTileEntity(tileEntityIn);
            trackedTileEntities.put(GuidebookLevel.packPos(x, y, z), tileEntityIn);
        }
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        TileEntity existing = level.getTileEntity(x, y, z);
        unregisterTrackedTileEntity(x, y, z, existing);
        level.setTileEntity(x, y, z, null);
        if (existing != null) {
            existing.invalidate();
        }
    }

    @Override
    public boolean func_147480_a(int x, int y, int z, boolean dropBlock) {
        Block existing = level.getBlock(x, y, z);
        if (existing == Blocks.air) {
            return false;
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        level.setBlock(x, y, z, Blocks.air, 0, null);
        if (tileEntity != null) {
            tileEntity.invalidate();
        }
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block blockType) {
        return setBlock(x, y, z, blockType, 0, 3);
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block blockIn, int metadataIn, int flags) {
        TileEntity tileEntity = null;
        if (blockIn != null && blockIn != Blocks.air && blockIn.hasTileEntity(metadataIn)) {
            tileEntity = GuidebookTileEntityLoader.load(this, blockIn, metadataIn, x, y, z, null);
        }
        level.setBlock(x, y, z, blockIn, metadataIn, tileEntity);
        if (blockIn != null && blockIn != Blocks.air) {
            try {
                blockIn.onBlockAdded(this, x, y, z);
            } catch (Throwable t) {
                GuideDebugLog
                    .warn("Preview block onBlockAdded hook failed at ({}, {}, {}) for {}", x, y, z, blockIn, t);
            }
        }
        return true;
    }

    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, int flags) {
        return level != null && level.setBlockMetadata(x, y, z, metadata);
    }

    @Override
    public void updateEntities() {}

    public void updateEntitiesForPreview() {
        super.updateEntities();
        GregTechHelpers.refreshPreviewClientEvents(loadedTileEntityList);
    }

    public void syncLoadedTileEntities(Collection<TileEntity> tileEntities) {
        loadedTileEntityList.clear();
        trackedTileEntities.clear();
        if (tileEntities == null || tileEntities.isEmpty()) {
            return;
        }
        for (TileEntity tileEntity : tileEntities) {
            if (tileEntity == null || tileEntity.isInvalid()) {
                continue;
            }
            addTileEntity(tileEntity);
            trackedTileEntities
                .put(GuidebookLevel.packPos(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord), tileEntity);
        }
    }

    public void syncLoadedEntities(Collection<Entity> entities) {
        loadedEntityList.clear();
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (Entity entity : entities) {
            if (entity == null || entity.isDead) {
                continue;
            }
            entity.worldObj = this;
            entity.dimension = provider.dimensionId;
            loadedEntityList.add(entity);
        }
    }

    private void unregisterTrackedTileEntity(int x, int y, int z, TileEntity exactTileEntity) {
        long key = GuidebookLevel.packPos(x, y, z);
        Iterator<TileEntity> iterator = loadedTileEntityList.iterator();
        while (iterator.hasNext()) {
            TileEntity tracked = iterator.next();
            if (tracked == null) {
                iterator.remove();
                continue;
            }
            if (exactTileEntity != null ? tracked == exactTileEntity
                : tracked.xCoord == x && tracked.yCoord == y && tracked.zCoord == z) {
                iterator.remove();
            }
        }
        TileEntity tracked = trackedTileEntities.get(key);
        if (exactTileEntity == null || tracked == exactTileEntity) {
            trackedTileEntities.remove(key);
        }
    }

    private void applyDescriptionPacketToTileEntity(TileEntity tileEntity) {
        if (tileEntity == null) {
            return;
        }
        if (suppressAe2StaleTileDescriptionRefresh(tileEntity)) {
            return;
        }
        try {
            Packet packet = tileEntity.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tileEntity.onDataPacket(null, updatePacket);
            }
        } catch (Throwable failure) {
            GuideDebugLog.warn(
                "Preview TileEntity description stream failed at ({}, {}, {}) for {}",
                tileEntity.xCoord,
                tileEntity.yCoord,
                tileEntity.zCoord,
                tileEntity.getClass()
                    .getName(),
                failure);
        }
    }

    private boolean suppressAe2StaleTileDescriptionRefresh(@Nullable TileEntity te) {
        if (level == null || te == null) {
            return false;
        }
        return GuideNhIntegrationRegistry.global()
            .suppressMarkBlockForUpdateDescriptionResync(te, level);
    }

    private Set<TileEntity> getOrCreateMarkBlockForUpdateGuard() {
        if (markBlockForUpdateGuard == null) {
            markBlockForUpdateGuard = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        return markBlockForUpdateGuard;
    }

}
