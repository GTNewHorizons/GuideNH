package com.hfstudio.guidenh.capture.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiProvider;
import com.hfstudio.guidenh.capture.server.api.CaptureServerQuery;
import com.hfstudio.guidenh.capture.server.api.CaptureServerReopen;
import com.hfstudio.guidenh.capture.server.api.CaptureServerReply;
import com.hfstudio.guidenh.capture.server.api.CaptureServerRequest;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.capture.Ae2ServerGuiProvider;
import com.hfstudio.guidenh.integration.capture.VanillaServerGuiProvider;
import com.hfstudio.guidenh.integration.gregtech.capture.GregTechServerGuiProvider;
import com.hfstudio.guidenh.integration.mui.capture.MuiServerGuiProvider;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2ServerContainerIdentity;
import com.hfstudio.guidenh.network.capture.CaptureCapabilityMessage;
import com.hfstudio.guidenh.network.capture.CaptureNetwork;
import com.hfstudio.guidenh.network.capture.CaptureReplyMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CaptureServerCoordinator {

    private static final CaptureServerCoordinator INSTANCE = new CaptureServerCoordinator();
    private final CaptureServerRegistry registry = new CaptureServerRegistry();
    private final ArrayBlockingQueue<QueuedRequest> requests = new ArrayBlockingQueue<>(256);
    private final Map<UUID, ServerCatalog> catalogs = new HashMap<>();
    private final Map<UUID, NavigationLease> leases = new HashMap<>();
    private final Map<UUID, OriginFactory> origins = new HashMap<>();
    private long tick;

    public static CaptureServerCoordinator instance() {
        return INSTANCE;
    }

    public CaptureServerRegistry registry() {
        return registry;
    }

    public void initialize() {
        registry.register(new VanillaServerGuiProvider());
        if (Mods.GregTech.isModLoaded()) registry.register(new GregTechServerGuiProvider());
        if (Mods.AE2.isModLoaded()) registry.register(new Ae2ServerGuiProvider());
        if (Mods.ModularUI.isModLoaded()) registry.register(new MuiServerGuiProvider());
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    public void enqueue(UUID playerId, CaptureServerRequest request) {
        requests.offer(new QueuedRequest(playerId, request));
    }

    public void recordOrigin(EntityPlayerMP player, CaptureServerReopen factory) {
        if (player.openContainer != player.inventoryContainer)
            origins.put(player.getUniqueID(), new OriginFactory(player.openContainer, factory));
    }

    public void forgetOrigin(EntityPlayerMP player) {
        origins.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        tick++;
        for (int count = 0; count < 16; count++) {
            QueuedRequest queued = requests.poll();
            if (queued == null) break;
            EntityPlayerMP player = player(server, queued.playerId());
            if (player == null) continue;
            try {
                handle(server, player, queued.request());
            } catch (RuntimeException error) {
                reply(
                    player,
                    queued.request()
                        .id(),
                    "failed",
                    error.toString(),
                    -1);
            }
        }
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            UUID id = player.getUniqueID();
            ServerCatalog catalog = catalogs.get(id);
            if (catalog != null) {
                if (tick - catalog.lastUsed > 6000 && !leases.containsKey(id)) catalogs.remove(id);
                else if (!catalog.complete) {
                    try {
                        scan(player, catalog);
                    } catch (RuntimeException error) {
                        catalog.complete = true;
                        reply(player, catalog.id, "failed", error.toString(), -1);
                    }
                }
            }
            NavigationLease lease = leases.get(id);
            if (lease == null) continue;
            if (tick - lease.lastUsed > 1200 || player.isDead || player.dimension != lease.dimension) {
                release(player, false);
                continue;
            }
            if (lease.pending != null && tick >= lease.readyTick) {
                UUID pending = lease.pending;
                lease.pending = null;
                if (player.openContainer != lease.active || !lease.active.canInteractWith(player))
                    reply(player, pending, "unavailable", "The container closed before synchronization completed", -1);
                else {
                    player.openContainer.detectAndSendChanges();
                    player.sendContainerToPlayer(player.openContainer);
                    reply(player, pending, "ready", "", player.openContainer.windowId);
                }
                if (lease.restoring) leases.remove(id);
            }
        }
    }

    private EntityPlayerMP player(MinecraftServer server, UUID id) {
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (player.getUniqueID()
                .equals(id)) return player;
        }
        return null;
    }

    private CaptureServerContext context(MinecraftServer server, EntityPlayerMP player, CaptureServerQuery query) {
        return new CaptureServerContext(player, (WorldServer) player.worldObj, query, server.isSinglePlayer());
    }

    private void handle(MinecraftServer server, EntityPlayerMP player, CaptureServerRequest request) {
        UUID playerId = player.getUniqueID();
        if (request.action()
            .equals("capability")) {
            CaptureNetwork.channel()
                .sendTo(new CaptureCapabilityMessage(server.isSinglePlayer()), player);
            return;
        }
        if (request.action()
            .equals("catalog")) {
            if (leases.containsKey(playerId))
                throw new IllegalStateException("Finish the current server navigation first");
            CaptureServerQuery query = request.query();
            if (query.radius() < 1 || query.radius() > CaptureCapabilityMessage.MAX_RADIUS
                || query.maxTargets() < 1
                || query.maxTargets() > CaptureCapabilityMessage.MAX_TARGETS)
                throw new IllegalArgumentException("Server capture query exceeds the advertised bounds");
            if (query.temporaryTravel() && (!server.isSinglePlayer() || !player.capabilities.isCreativeMode))
                throw new IllegalArgumentException(
                    "Temporary travel requires a creative player in an integrated server");
            OriginFactory original = origins.get(playerId);
            catalogs.put(
                playerId,
                new ServerCatalog(
                    request.id(),
                    context(server, player, query),
                    tick,
                    original != null && original.container() == player.openContainer ? original.factory() : null));
            return;
        }
        NavigationLease lease = leases.get(playerId);
        if (request.action()
            .equals("heartbeat")) {
            if (lease != null && lease.catalog.id.equals(request.catalogId())) lease.lastUsed = tick;
            return;
        }
        if (request.action()
            .equals("restore")
            || request.action()
                .equals("finish")) {
            if (lease == null) {
                reply(player, request.id(), "ready", "", player.openContainer.windowId);
                return;
            }
            if (!lease.catalog.id.equals(request.catalogId()))
                throw new IllegalArgumentException("Navigation session changed");
            restore(
                server,
                player,
                lease,
                request.id(),
                request.action()
                    .equals("restore"));
            return;
        }
        if (!request.action()
            .equals("open")) throw new IllegalArgumentException("Unknown capture navigation action");
        ServerCatalog catalog = catalogs.get(playerId);
        if (catalog == null || !catalog.complete || !catalog.id.equals(request.catalogId()))
            throw new IllegalStateException("Refresh the server GUI catalog before navigating");
        ServerEntry entry = catalog.entries.get(request.targetId());
        if (entry == null || !entry.target()
            .available()) throw new IllegalArgumentException("The catalog target is unavailable");
        if (player.dimension != catalog.context.world().provider.dimensionId)
            throw new IllegalStateException("The player's dimension changed");
        if (lease == null) {
            if (player.openContainer != catalog.origin)
                throw new IllegalStateException("The original container changed; refresh the catalog");
            if (player.inventory.getItemStack() != null)
                throw new IllegalStateException("Put down the carried item before navigating");
            if (catalog.origin != player.inventoryContainer && catalog.original == null && catalog.reopen == null)
                throw new IllegalStateException("The original container has no registered restoration factory");
            lease = new NavigationLease(catalog, player, tick);
            leases.put(playerId, lease);
        }
        if (lease.pending != null) throw new IllegalStateException("The previous container is still synchronizing");
        if (lease.active != null && player.openContainer != player.inventoryContainer
            && player.openContainer != lease.active)
            throw new IllegalStateException("The container changed outside the capture batch");
        catalog.lastUsed = lease.lastUsed = tick;
        open(server, player, lease, entry, request.id());
    }

    private void scan(EntityPlayerMP player, ServerCatalog catalog) {
        List<CaptureServerTarget> page = new ArrayList<>();
        int visited = 0;
        while (catalog.cursor < catalog.tiles.size() && visited++ < 128
            && catalog.entries.size() < catalog.context.query()
                .maxTargets()) {
            TileEntity tile = catalog.tiles.get(catalog.cursor++);
            if (tile.isInvalid() || tile.getWorldObj() != catalog.context.world()) continue;
            CaptureServerGuiProvider provider = registry.provider(tile);
            List<String> variants = provider == null ? List.of("unknown") : provider.variants(tile);
            for (String variant : variants) {
                if (catalog.entries.size() >= catalog.context.query()
                    .maxTargets()) break;
                String providerId = provider == null ? "unsupported" : provider.id();
                String id = providerId + ":" + tile.xCoord + "," + tile.yCoord + "," + tile.zCoord + ":" + variant;
                String reason;
                try {
                    reason = provider == null ? "No registered GUI factory for this loaded tile"
                        : provider.accessReason(catalog.context, tile, variant);
                } catch (RuntimeException error) {
                    reason = error.toString();
                }
                if (reason.isEmpty() && !catalog.context.query()
                    .temporaryTravel() && distance(player, tile) >= 64)
                    reason = "This target is outside interaction distance";
                CaptureServerTarget target = new CaptureServerTarget(
                    id,
                    providerId,
                    player.dimension,
                    tile.xCoord,
                    tile.yCoord,
                    tile.zCoord,
                    variant,
                    provider == null ? tile.getClass()
                        .getSimpleName() : provider.label(tile, variant),
                    reason.isEmpty() ? "ready" : provider == null ? "unsupported" : "unavailable",
                    reason);
                ServerEntry entry = new ServerEntry(target, tile);
                catalog.entries.put(id, entry);
                page.add(target);
                if (provider != null && catalog.original == null
                    && (tile == catalog.originTile || provider.owns(catalog.context, tile, catalog.origin, variant)))
                    catalog.original = entry;
            }
        }
        catalog.complete = catalog.cursor >= catalog.tiles.size() || catalog.entries.size() >= catalog.context.query()
            .maxTargets();
        boolean limited = catalog.complete && catalog.cursor < catalog.tiles.size();
        for (int offset = 0; offset < page.size() || offset == 0 && page.isEmpty() && catalog.complete; offset += 128) {
            boolean last = offset + 128 >= page.size();
            send(
                player,
                new CaptureServerReply(
                    catalog.id,
                    limited ? "limited" : "catalog",
                    limited ? "The configured server target limit was reached" : "",
                    -1,
                    page.subList(offset, Math.min(offset + 128, page.size())),
                    catalog.complete && last));
        }
    }

    private double distance(EntityPlayerMP player, TileEntity tile) {
        return player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5);
    }

    private void open(MinecraftServer server, EntityPlayerMP player, NavigationLease lease, ServerEntry entry,
        UUID requestId) {
        TileEntity tile = entry.tile();
        CaptureServerContext context = context(server, player, lease.catalog.context.query());
        if (tile.isInvalid() || tile.getWorldObj() != player.worldObj
            || !context.world()
                .blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
            || context.world()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)
            throw new IllegalStateException("The target tile was removed or replaced");
        if (distance(player, tile) >= 64) {
            if (!context.query()
                .temporaryTravel() || !context.integrated() || !player.capabilities.isCreativeMode)
                throw new IllegalStateException("This target is outside interaction distance");
            if (player.openContainer != player.inventoryContainer) player.closeScreen();
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.playerNetServerHandler.setPlayerLocation(
                tile.xCoord + 0.5,
                tile.yCoord + 1.5,
                tile.zCoord + 0.5,
                player.rotationYaw,
                player.rotationPitch);
            player.fallDistance = 0;
            player.motionX = player.motionY = player.motionZ = 0;
            lease.travelled = true;
            reply(player, requestId, "positioned", "Waiting for the target chunk on the client", -1);
            return;
        }
        CaptureServerGuiProvider provider = registry.provider(
            entry.target()
                .providerId());
        String access = provider.accessReason(
            context,
            tile,
            entry.target()
                .variant());
        if (!access.isEmpty()) throw new IllegalStateException(access);
        if (player.openContainer != player.inventoryContainer) player.closeScreen();
        provider.open(
            context,
            tile,
            entry.target()
                .variant());
        if (player.openContainer == player.inventoryContainer || !player.openContainer.canInteractWith(player)) {
            if (player.openContainer != player.inventoryContainer) player.closeScreen();
            throw new IllegalStateException("The registered factory did not open a usable container");
        }
        lease.active = player.openContainer;
        lease.pending = requestId;
        lease.readyTick = tick + 3;
    }

    private void restore(MinecraftServer server, EntityPlayerMP player, NavigationLease lease, UUID requestId,
        boolean reopen) {
        lease.pending = null;
        if ((reopen || lease.travelled) && player.openContainer == lease.active
            && player.openContainer != player.inventoryContainer) player.closeScreen();
        restorePosition(player, lease);
        lease.lastUsed = tick;
        try {
            if (reopen && (lease.catalog.reopen != null || lease.catalog.original != null)) {
                if (player.openContainer != player.inventoryContainer && player.openContainer != lease.active)
                    throw new IllegalStateException("Another container replaced the capture target");
                if (lease.catalog.reopen != null) {
                    CaptureServerReopen factory = lease.catalog.reopen;
                    TileEntity tile = factory.source();
                    CaptureServerContext context = context(server, player, lease.catalog.context.query());
                    if (tile.isInvalid() || tile.getWorldObj() != context.world()
                        || !context.world()
                            .blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
                        || context.world()
                            .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)
                        throw new IllegalStateException("The original tile was removed or replaced");
                    CaptureServerGuiProvider provider = registry.provider(tile);
                    String access = provider == null ? ""
                        : provider.accessReason(
                            context,
                            tile,
                            provider.variants(tile)
                                .get(0));
                    if (access.isEmpty()) access = factory.accessReason(context);
                    if (!access.isEmpty()) throw new IllegalStateException(access);
                    factory.open(context);
                    lease.active = player.openContainer;
                    if (lease.active == player.inventoryContainer || !lease.active.canInteractWith(player))
                        throw new IllegalStateException("The original factory did not reopen a usable container");
                    lease.pending = requestId;
                    lease.readyTick = tick + 3;
                } else open(server, player, lease, lease.catalog.original, requestId);
                lease.restoring = true;
            } else {
                leases.remove(player.getUniqueID());
                player.sendContainerToPlayer(player.openContainer);
                reply(player, requestId, "ready", "", player.openContainer.windowId);
            }
        } catch (RuntimeException error) {
            leases.remove(player.getUniqueID());
            reply(player, requestId, "failed", "Could not reopen the original container: " + error, -1);
        }
    }

    private void restorePosition(EntityPlayerMP player, NavigationLease lease) {
        if (!lease.travelled) return;
        if (player.dimension == lease.dimension && !player.isDead) {
            player.playerNetServerHandler.setPlayerLocation(lease.x, lease.y, lease.z, lease.yaw, lease.pitch);
            player.capabilities.isFlying = lease.flying;
            player.sendPlayerAbilities();
            player.motionX = lease.motionX;
            player.motionY = lease.motionY;
            player.motionZ = lease.motionZ;
            player.fallDistance = lease.fallDistance;
        } else {
            player.capabilities.isFlying = lease.flying;
            player.sendPlayerAbilities();
        }
        lease.travelled = false;
    }

    public void release(EntityPlayerMP player, boolean reopen) {
        NavigationLease lease = leases.get(player.getUniqueID());
        if (lease != null) restore(
            FMLCommonHandler.instance()
                .getMinecraftServerInstance(),
            player,
            lease,
            UUID.randomUUID(),
            reopen);
        catalogs.remove(player.getUniqueID());
        origins.remove(player.getUniqueID());
        requests.removeIf(
            request -> request.playerId()
                .equals(player.getUniqueID()));
    }

    public void stop() {
        requests.clear();
        catalogs.clear();
        leases.clear();
        origins.clear();
        tick = 0;
    }

    private void reply(EntityPlayerMP player, UUID requestId, String status, String reason, int window) {
        send(player, new CaptureServerReply(requestId, status, reason, window, List.of(), true));
    }

    private void send(EntityPlayerMP player, CaptureServerReply reply) {
        CaptureNetwork.channel()
            .sendTo(new CaptureReplyMessage(reply), player);
    }

    public record QueuedRequest(UUID playerId, CaptureServerRequest request) {}

    public record ServerEntry(CaptureServerTarget target, TileEntity tile) {}

    public record OriginFactory(Container container, CaptureServerReopen factory) {}

    public static class ServerCatalog {

        private final UUID id;
        private final CaptureServerContext context;
        private final List<TileEntity> tiles = new ArrayList<>();
        private final Map<String, ServerEntry> entries = new LinkedHashMap<>();
        private final Container origin;
        private final TileEntity originTile;
        private final CaptureServerReopen reopen;
        private ServerEntry original;
        private int cursor;
        private boolean complete;
        private long lastUsed;

        public ServerCatalog(UUID id, CaptureServerContext context, long tick, CaptureServerReopen reopen) {
            this.id = id;
            this.context = context;
            this.reopen = reopen;
            lastUsed = tick;
            origin = context.player().openContainer;
            originTile = Mods.ModularUI2.isModLoaded() ? Mui2ServerContainerIdentity.tile(origin) : null;
            int radius = context.query()
                .radius();
            for (Object value : context.world().loadedTileEntityList) {
                TileEntity tile = (TileEntity) value;
                if (Math.abs(tile.xCoord + 0.5 - context.player().posX) <= radius
                    && Math.abs(tile.yCoord + 0.5 - context.player().posY) <= radius
                    && Math.abs(tile.zCoord + 0.5 - context.player().posZ) <= radius) tiles.add(tile);
            }
            tiles.sort(
                Comparator.comparingDouble(
                    tile -> context.player()
                        .getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5)));
        }
    }

    public static class NavigationLease {

        private final ServerCatalog catalog;
        private final int dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final boolean flying;
        private final double motionX;
        private final double motionY;
        private final double motionZ;
        private final float fallDistance;
        private Container active;
        private UUID pending;
        private long readyTick;
        private long lastUsed;
        private boolean travelled;
        private boolean restoring;

        public NavigationLease(ServerCatalog catalog, EntityPlayerMP player, long tick) {
            this.catalog = catalog;
            dimension = player.dimension;
            x = player.posX;
            y = player.posY;
            z = player.posZ;
            yaw = player.rotationYaw;
            pitch = player.rotationPitch;
            flying = player.capabilities.isFlying;
            motionX = player.motionX;
            motionY = player.motionY;
            motionZ = player.motionZ;
            fallDistance = player.fallDistance;
            lastUsed = tick;
        }
    }
}
