package com.hfstudio.guidenh.client.capture.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;

import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.capture.server.api.CaptureServerReply;
import com.hfstudio.guidenh.capture.server.api.CaptureServerRequest;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;
import com.hfstudio.guidenh.client.GuideNhClientTaskScheduler;
import com.hfstudio.guidenh.integration.capture.CaptureChannelRegistrationBridge;
import com.hfstudio.guidenh.network.capture.CaptureCapabilityMessage;
import com.hfstudio.guidenh.network.capture.CaptureNetwork;
import com.hfstudio.guidenh.network.capture.CaptureRequestMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

public class CaptureClientConnection {

    private static final CaptureClientConnection INSTANCE = new CaptureClientConnection();
    private final Map<UUID, PendingNavigation> pending = new HashMap<>();
    private final List<CaptureServerTarget> targets = new ArrayList<>();
    private final long[] synchronizedWindows = new long[101];
    private INetHandler server;
    private NetworkManager advertisedConnection;
    private boolean capabilityRequested;
    private boolean capabilityPending;
    private long capabilityStarted;
    private boolean integrated;
    private CaptureServerQuery query;
    private UUID catalogId;
    private UUID navigationCatalog;
    private GuiScreen catalogScreen;
    private boolean catalogReady;
    private String reason = "";
    private long catalogRequested;
    private long lastHeartbeat;
    private long synchronizationVersion;
    private Runnable catalogChanged = () -> {};

    public static CaptureClientConnection instance() {
        return INSTANCE;
    }

    public void onCatalogChanged(Runnable callback) {
        catalogChanged = callback;
    }

    public void capability(INetHandler source, int version, boolean integrated) {
        GuideNhClientTaskScheduler.execute(() -> {
            if (Minecraft.getMinecraft()
                .getNetHandler() != source) return;
            disconnect();
            if (version == CaptureCapabilityMessage.VERSION) {
                server = source;
                this.integrated = integrated;
            } else reason = "The server uses a different capture protocol version";
            catalogChanged.run();
        });
    }

    public boolean available() {
        return server != null && server == Minecraft.getMinecraft()
            .getNetHandler();
    }

    public boolean integrated() {
        return integrated;
    }

    public UUID catalogId() {
        return catalogId;
    }

    public List<CaptureServerTarget> targets() {
        return List.copyOf(targets);
    }

    public String reason() {
        return reason;
    }

    public boolean catalogReady() {
        return catalogReady;
    }

    public void ensureCatalog(CaptureServerQuery requested) {
        if (!available() || navigationCatalog != null) return;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (requested.equals(query) && catalogScreen == screen
            && System.nanoTime() - catalogRequested < (catalogReady ? 5_000_000_000L : 30_000_000_000L)) return;
        query = requested;
        catalogScreen = screen;
        targets.clear();
        catalogReady = false;
        reason = "Waiting for the server GUI catalog";
        catalogRequested = System.nanoTime();
        catalogId = UUID.randomUUID();
        send(new CaptureServerRequest(catalogId, catalogId, "catalog", "", requested));
    }

    public CompletableFuture<CaptureServerReply> navigate(UUID catalog, String action, String target) {
        if (!available())
            return CompletableFuture.failedFuture(new IllegalStateException("The capture server disconnected"));
        navigationCatalog = catalog;
        lastHeartbeat = System.nanoTime();
        UUID id = UUID.randomUUID();
        CompletableFuture<CaptureServerReply> result = new CompletableFuture<>();
        pending.put(id, new PendingNavigation(result, System.nanoTime() + 60_000_000_000L));
        result.whenComplete((reply, error) -> pending.remove(id));
        send(new CaptureServerRequest(id, catalog, action, target, query));
        return result;
    }

    public void receive(INetHandler source, CaptureServerReply reply) {
        GuideNhClientTaskScheduler.execute(() -> {
            if (source != server || source != Minecraft.getMinecraft()
                .getNetHandler()) return;
            if (reply.id()
                .equals(catalogId) && navigationCatalog == null) {
                targets.addAll(reply.targets());
                catalogReady = reply.complete();
                reason = reply.reason();
                if (catalogReady) catalogChanged.run();
            } else {
                PendingNavigation navigation = pending.remove(reply.id());
                if (navigation != null) navigation.future()
                    .complete(reply);
            }
        });
    }

    public void endNavigation(UUID catalog) {
        if (!catalog.equals(navigationCatalog)) return;
        navigationCatalog = null;
        catalogScreen = null;
    }

    public boolean needsServerTick() {
        return capabilityPending && Minecraft.getMinecraft()
            .isIntegratedServerRunning()
            || available() && integrated && (navigationCatalog != null || catalogId != null && !catalogReady);
    }

    public void closeCapture() {
        catalogReady = true;
        catalogScreen = null;
        catalogId = null;
        targets.clear();
        if (navigationCatalog == null) query = null;
    }

    public long synchronizationVersion() {
        return synchronizationVersion;
    }

    public void windowSynchronized(int window) {
        if (window >= 0 && window < synchronizedWindows.length) synchronizedWindows[window] = ++synchronizationVersion;
    }

    public boolean synchronizedAfter(int window, long version) {
        return window >= 0 && window < synchronizedWindows.length && synchronizedWindows[window] > version;
    }

    @SubscribeEvent
    public void onChannelRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side != Side.CLIENT || !event.operation.equals("REGISTER")
            || !CaptureChannelRegistrationBridge.contains(event, CaptureNetwork.CHANNEL_NAME)) return;
        NetworkManager connection = event.manager;
        GuideNhClientTaskScheduler.execute(() -> {
            if (Minecraft.getMinecraft()
                .getNetHandler() != null
                && Minecraft.getMinecraft()
                    .getNetHandler()
                    .getNetworkManager() != connection)
                return;
            if (advertisedConnection != connection) {
                advertisedConnection = connection;
                capabilityRequested = false;
            }
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        if (!available() && Minecraft.getMinecraft().thePlayer != null
            && advertisedConnection != null
            && !capabilityRequested
            && Minecraft.getMinecraft()
                .getNetHandler() != null
            && Minecraft.getMinecraft()
                .getNetHandler()
                .getNetworkManager() == advertisedConnection) {
            capabilityRequested = capabilityPending = true;
            capabilityStarted = now;
            UUID id = UUID.randomUUID();
            send(new CaptureServerRequest(id, id, "capability", "", new CaptureServerQuery(8, 500, false)));
        }
        if (capabilityPending && now - capabilityStarted >= 30_000_000_000L) {
            capabilityPending = false;
            reason = "Server capture capability negotiation timed out";
        }
        for (PendingNavigation navigation : List.copyOf(pending.values())) {
            if (now >= navigation.deadline()) navigation.future()
                .completeExceptionally(new IllegalStateException("Server capture navigation timed out"));
        }
        if (catalogId != null && !catalogReady && now - catalogRequested >= 30_000_000_000L) {
            catalogReady = true;
            reason = "Server GUI catalog timed out; refresh to retry";
            catalogChanged.run();
        }
        if (navigationCatalog == null || !available()) return;
        if (now - lastHeartbeat >= 1_000_000_000L) {
            lastHeartbeat = now;
            send(new CaptureServerRequest(UUID.randomUUID(), navigationCatalog, "heartbeat", "", query));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        INetHandler disconnected = event.handler;
        NetworkManager connection = event.manager;
        GuideNhClientTaskScheduler
            .execute(() -> { if (server == disconnected || advertisedConnection == connection) disconnect(); });
    }

    private void disconnect() {
        server = null;
        advertisedConnection = null;
        capabilityRequested = capabilityPending = false;
        integrated = false;
        for (PendingNavigation navigation : List.copyOf(pending.values())) navigation.future()
            .completeExceptionally(new IllegalStateException("The capture server disconnected"));
        pending.clear();
        targets.clear();
        query = null;
        catalogId = navigationCatalog = null;
        catalogScreen = null;
        catalogReady = false;
        reason = "";
    }

    private void send(CaptureServerRequest request) {
        CaptureNetwork.channel()
            .sendToServer(new CaptureRequestMessage(request));
    }

    public record PendingNavigation(CompletableFuture<CaptureServerReply> future, long deadline) {}
}
