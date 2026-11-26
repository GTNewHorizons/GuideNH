package com.hfstudio.guidenh.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideNhServerRequests {

    private static final GuideNhServerRequests INSTANCE = new GuideNhServerRequests();
    private static final long MAX_QUEUED_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_REQUESTS = 1024;
    private static final int MAX_PLAYER_REQUESTS = 512;
    private static final int MAX_INCREMENTAL_REQUESTS = 4;
    private static final int MAX_REQUESTS_PER_TICK = 16;
    private static final long TICK_BUDGET_NANOS = 2_000_000L;
    private final ArrayBlockingQueue<Request> requests = new ArrayBlockingQueue<>(MAX_REQUESTS);
    private final Map<UUID, Integer> playerCounts = new HashMap<>();
    private long queuedBytes;
    private int requestCount;
    private int incrementalCount;

    public static GuideNhServerRequests instance() {
        return INSTANCE;
    }

    public synchronized boolean enqueue(EntityPlayerMP player, int bytes, Consumer<EntityPlayerMP> action) {
        return enqueue(player, bytes, (queuedPlayer, deadline) -> {
            action.accept(queuedPlayer);
            return true;
        }, false);
    }

    public synchronized boolean enqueueWork(EntityPlayerMP player, int bytes, GuideNhServerWork work) {
        return incrementalCount < MAX_INCREMENTAL_REQUESTS && enqueue(player, bytes, work, true);
    }

    private boolean enqueue(EntityPlayerMP player, int bytes, GuideNhServerWork work, boolean incremental) {
        if (player == null || bytes < 0 || bytes > MAX_QUEUED_BYTES - queuedBytes || requestCount >= MAX_REQUESTS)
            return false;
        UUID playerId = player.getUniqueID();
        int count = playerCounts.getOrDefault(playerId, 0);
        if (count >= MAX_PLAYER_REQUESTS
            || !requests.offer(new Request(player, player.playerNetServerHandler, playerId, bytes, work, incremental)))
            return false;
        queuedBytes += bytes;
        requestCount++;
        if (incremental) incrementalCount++;
        playerCounts.put(playerId, count + 1);
        return true;
    }

    private synchronized Request poll() {
        return requests.poll();
    }

    private synchronized void release(Request request) {
        queuedBytes -= request.bytes();
        requestCount--;
        if (request.incremental()) incrementalCount--;
        int count = playerCounts.get(request.playerId()) - 1;
        if (count == 0) playerCounts.remove(request.playerId());
        else playerCounts.put(request.playerId(), count);
    }

    private static void close(Request request) {
        try {
            request.work()
                .close();
        } catch (RuntimeException error) {
            GuideDebugLog.warnAlways("GuideNH server request cleanup failed", error);
        }
    }

    public synchronized void clear(UUID playerId) {
        requests.removeIf(request -> {
            if (!request.playerId()
                .equals(playerId)) return false;
            release(request);
            close(request);
            return true;
        });
    }

    public synchronized void clear() {
        requests.forEach(GuideNhServerRequests::close);
        requests.clear();
        playerCounts.clear();
        queuedBytes = 0L;
        requestCount = 0;
        incrementalCount = 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        for (int count = 0; count < MAX_REQUESTS_PER_TICK && System.nanoTime() < deadline; count++) {
            Request request = poll();
            if (request == null) break;
            boolean complete = true;
            EntityPlayerMP player = request.player();
            try {
                if (player.playerNetServerHandler == request.connection()
                    && server.getConfigurationManager().playerEntityList.contains(player)) {
                    complete = request.work()
                        .step(player, deadline);
                }
            } catch (RuntimeException error) {
                GuideDebugLog.warnAlways("GuideNH server request failed", error);
            } finally {
                if (complete || !requests.offer(request)) {
                    release(request);
                    close(request);
                }
            }
        }
    }

    public record Request(EntityPlayerMP player, NetHandlerPlayServer connection, UUID playerId, int bytes,
        GuideNhServerWork work, boolean incremental) {}
}
