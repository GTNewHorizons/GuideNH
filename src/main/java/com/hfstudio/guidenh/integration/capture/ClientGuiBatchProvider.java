package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureClientGuiFactory;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class ClientGuiBatchProvider implements CaptureBatchProvider {

    private final Map<String, CaptureClientGuiFactory> factories = new LinkedHashMap<>();
    private final Map<String, String> observed = new LinkedHashMap<>();

    public void register(CaptureClientGuiFactory factory) {
        factories.put(factory.id(), factory);
    }

    public void observe(GuiScreen screen) {
        observed.put(
            screen.getClass()
                .getName(),
            screen.getClass()
                .getSimpleName());
    }

    @Override
    public String id() {
        return "client";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() != null;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.REACHABLE)) return List.of();
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (CaptureClientGuiFactory factory : factories.values()) {
            boolean container = context.capture()
                .screen() instanceof GuiContainer;
            boolean available = !container && factory.available()
                .test(context);
            targets.add(
                new CaptureTargetDescriptor(
                    "client:" + factory.id(),
                    id(),
                    CaptureBatchOptions.REACHABLE,
                    factory.label(),
                    available ? "ready" : "unavailable",
                    container ? "The original container requires a registered reopening provider"
                        : available ? "" : factory.unavailableReason(),
                    Map.of("factory", factory.id())));
        }
        Set<String> known = factories.values()
            .stream()
            .map(
                factory -> factory.screenType()
                    .getName())
            .collect(Collectors.toSet());
        for (Map.Entry<String, String> entry : observed.entrySet()) {
            boolean current = context.capture()
                .screen()
                .getClass()
                .getName()
                .equals(entry.getKey());
            if (!current && known.contains(entry.getKey())) continue;
            targets.add(
                new CaptureTargetDescriptor(
                    "observed:" + entry.getKey(),
                    id(),
                    CaptureBatchOptions.REACHABLE,
                    entry.getValue(),
                    current ? "ready" : "unsupported",
                    current ? "" : "This GUI was observed but has no registered opening factory",
                    Map.of("observed", entry.getKey())));
        }
        return targets;
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new ClientNavigator(
            context.capture()
                .screen(),
            factories);
    }

    public static class ClientNavigator implements CaptureBatchNavigator {

        private final GuiScreen origin;
        private final Map<String, CaptureClientGuiFactory> factories;
        private GuiScreen selected;

        public ClientNavigator(GuiScreen origin, Map<String, CaptureClientGuiFactory> factories) {
            this.origin = origin;
            this.factories = Map.copyOf(factories);
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            if (target.data()
                .containsKey("observed")) selected = origin;
            else selected = factories.get(
                target.data()
                    .get("factory"))
                .create()
                .apply(origin);
            if (Minecraft.getMinecraft().currentScreen != selected) Minecraft.getMinecraft()
                .displayGuiScreen(selected);
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = selected != null && context.capture()
                .screen() == selected;
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the client GUI to open");
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
            if (origin instanceof GuiContainer && Minecraft.getMinecraft().currentScreen != origin) {
                throw new IllegalStateException(
                    "The original container closed and needs a registered reopening provider");
            }
            if (Minecraft.getMinecraft().currentScreen != origin) Minecraft.getMinecraft()
                .displayGuiScreen(origin);
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == origin;
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the original client GUI");
        }
    }
}
