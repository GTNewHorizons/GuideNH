package com.hfstudio.guidenh.capture.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;

import com.hfstudio.guidenh.capture.server.api.CaptureServerArea;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;

public class CaptureLoadedTiles {

    private CaptureLoadedTiles() {}

    public static List<TileEntity> collect(CaptureServerContext context) {
        CaptureServerQuery query = context.query();
        CaptureServerArea area = query.area();
        double originX = context.player().posX;
        double originY = context.player().posY;
        double originZ = context.player().posZ;
        int minX = area == null ? (int) Math.floor(originX - query.radius()) : area.minX();
        int maxX = area == null ? (int) Math.floor(originX + query.radius()) : area.maxX();
        int minZ = area == null ? (int) Math.floor(originZ - query.radius()) : area.minZ();
        int maxZ = area == null ? (int) Math.floor(originZ + query.radius()) : area.maxZ();
        List<TileEntity> tiles = new ArrayList<>();
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                // The loaded chunk map includes stationary tiles without loading or generating chunks.
                Chunk chunk = (Chunk) context.world().theChunkProviderServer.loadedChunkHashMap
                    .getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
                if (chunk == null) continue;
                for (TileEntity tile : chunk.chunkTileEntityMap.values()) {
                    if (!tile.isInvalid() && tile.getWorldObj() == context.world()
                        && query.includes(
                            tile.xCoord + 0.5,
                            tile.yCoord + 0.5,
                            tile.zCoord + 0.5,
                            originX,
                            originY,
                            originZ))
                        tiles.add(tile);
                }
            }
        }
        return List.copyOf(tiles);
    }
}
