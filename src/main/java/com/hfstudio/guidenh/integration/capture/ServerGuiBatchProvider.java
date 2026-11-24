package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import com.hfstudio.guidenh.capture.server.api.CaptureServerReply;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;
import com.hfstudio.guidenh.client.capture.ui.CaptureNavigationScreen;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2ClientContainerReadiness;

public class ServerGuiBatchProvider implements CaptureBatchProvider {

    private final CaptureClientConnection connection;

    public ServerGuiBatchProvider(CaptureClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public String id() {
        return "server_guis";
    }

    @Override
    public int navigationOrder() {
        return 100;
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return Minecraft.getMinecraft().thePlayer != null;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.REACHABLE)) return List.of();
        if (!connection.available()) return List.of(status("unavailable", "guidenh.capture.server_missing"));
        connection.ensureCatalog(options.serverQuery());
        if (!connection.catalogReady()) return List.of(status("loading", "guidenh.capture.server_loading"));
        if (connection.targets()
            .isEmpty())
            return List.of(
                new CaptureTargetDescriptor(
                    "server:empty",
                    id(),
                    CaptureBatchOptions.REACHABLE,
                    I18n.format("guidenh.capture.server_empty"),
                    "unavailable",
                    connection.reason(),
                    Map.of()));
        List<CaptureTargetDescriptor> targets = new ArrayList<>(
            connection.targets()
                .stream()
                .map(this::descriptor)
                .toList());
        if (!connection.reason()
            .isEmpty())
            targets.add(
                new CaptureTargetDescriptor(
                    "server:notice",
                    id(),
                    CaptureBatchOptions.REACHABLE,
                    I18n.format("guidenh.capture.server_notice"),
                    "unavailable",
                    connection.reason(),
                    Map.of()));
        return List.copyOf(targets);
    }

    private CaptureTargetDescriptor status(String state, String text) {
        return new CaptureTargetDescriptor(
            "server:" + state,
            id(),
            CaptureBatchOptions.REACHABLE,
            I18n.format(text),
            state,
            connection.reason(),
            Map.of());
    }

    private CaptureTargetDescriptor descriptor(CaptureServerTarget target) {
        return new CaptureTargetDescriptor(
            "server:" + target.id(),
            id(),
            CaptureBatchOptions.REACHABLE,
            I18n.format(target.label()) + " (" + target.x() + ", " + target.y() + ", " + target.z() + ")",
            target.availability(),
            target.reason(),
            Map.of(
                "target",
                target.id(),
                "provider",
                target.providerId(),
                "dimension",
                Integer.toString(target.dimension()),
                "x",
                Integer.toString(target.x()),
                "y",
                Integer.toString(target.y()),
                "z",
                Integer.toString(target.z()),
                "variant",
                target.variant()));
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new ServerNavigator(
            connection,
            connection.catalogId(),
            context.capture()
                .screen());
    }

    public static class ServerNavigator implements CaptureBatchNavigator {

        private final CaptureClientConnection connection;
        private final UUID catalogId;
        private final GuiScreen origin;
        private CompletableFuture<CaptureServerReply> pending;
        private CaptureServerReply reply;
        private long synchronization;
        private long acceptedFrame;
        private boolean finishing;
        private boolean restoreScreen;
        private boolean ended;
        private CaptureTargetDescriptor selected;

        public ServerNavigator(CaptureClientConnection connection, UUID catalogId, GuiScreen origin) {
            this.connection = connection;
            this.catalogId = catalogId;
            this.origin = origin;
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selected = target;
            request(
                "open",
                target.data()
                    .get("target"));
        }

        private void request(String action, String target) {
            if (pending != null) pending.cancel(false);
            synchronization = connection.synchronizationVersion();
            reply = null;
            acceptedFrame = Long.MIN_VALUE;
            pending = connection.navigate(catalogId, action, target);
        }

        @Override
        public void tick(CaptureBatchContext context) {
            if (finishing || pending == null || !pending.isDone()) return;
            CaptureServerReply response = pending.join();
            if (!response.status()
                .equals("positioned")) return;
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.theWorld == null) throw new IllegalStateException("The capture world disconnected");
            int x = Integer.parseInt(
                selected.data()
                    .get("x"));
            int y = Integer.parseInt(
                selected.data()
                    .get("y"));
            int z = Integer.parseInt(
                selected.data()
                    .get("z"));
            if (minecraft.theWorld.blockExists(x, y, z) && minecraft.theWorld.getTileEntity(x, y, z) != null) request(
                "open",
                selected.data()
                    .get("target"));
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            tick(context);
            if (pending == null || !pending.isDone()) return waiting(context, "Waiting for the server container");
            if (reply == null) {
                reply = pending.join();
                if (reply.status()
                    .equals("positioned")) {
                    reply = null;
                    return waiting(context, "Waiting for the target chunk on the client");
                }
                if (!reply.status()
                    .equals("ready")) throw new IllegalStateException(reply.reason());
                acceptedFrame = context.capture()
                    .frameId();
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.thePlayer == null) throw new IllegalStateException("The capture server disconnected");
            Container container = minecraft.thePlayer.openContainer;
            if (container.windowId != reply.windowId())
                return waiting(context, "Waiting for the matching container window");
            if (reply.windowId() == 0 && finishing) {
                if (restoreScreen
                    && (minecraft.currentScreen == null || minecraft.currentScreen instanceof CaptureNavigationScreen))
                    minecraft.displayGuiScreen(origin);
                if (!restoreScreen || context.capture()
                    .screen() == origin) return ready(context);
                return waiting(context, "Waiting for the original client GUI");
            }
            if (!connection.synchronizedAfter(reply.windowId(), synchronization))
                return waiting(context, "Waiting for the initial inventory snapshot");
            if (minecraft.currentScreen == null
                || minecraft.currentScreen instanceof GuiContainer gui && gui.inventorySlots != container)
                return waiting(context, "Waiting for the matching GUI");
            if (Mods.ModularUI2.isModLoaded() && !Mui2ClientContainerReadiness.ready(container))
                return waiting(context, "Waiting for MUI2 initialization");
            if (context.capture()
                .frameId() <= acceptedFrame) return waiting(context, "Waiting for the synchronized GUI to render");
            return ready(context);
        }

        private CaptureBatchReadiness ready(CaptureBatchContext context) {
            return new CaptureBatchReadiness(
                true,
                context.geometry()
                    .revision(),
                "");
        }

        private CaptureBatchReadiness waiting(CaptureBatchContext context, String reason) {
            return new CaptureBatchReadiness(
                false,
                context.geometry()
                    .revision(),
                reason);
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            return List.of(
                context.capture()
                    .viewport()
                    .bounds());
        }

        @Override
        public void restore() {
            finishing = restoreScreen = true;
            request("restore", "");
        }

        @Override
        public void finish() {
            finishing = true;
            restoreScreen = false;
            request("finish", "");
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            return finishReadiness(context);
        }

        @Override
        public CaptureBatchReadiness finishReadiness(CaptureBatchContext context) {
            CaptureBatchReadiness state = readiness(context);
            if (state.ready()) {
                ended = true;
                connection.endNavigation(catalogId);
            }
            return state;
        }

        @Override
        public void close() {
            if (!ended && connection.available()) connection.navigate(catalogId, "finish", "")
                .whenComplete((result, error) -> connection.endNavigation(catalogId));
            if (pending != null) pending.cancel(false);
        }
    }
}
