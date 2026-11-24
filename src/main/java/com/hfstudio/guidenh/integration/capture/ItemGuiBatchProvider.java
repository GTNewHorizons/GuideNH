package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureItemGuiContext;
import com.hfstudio.guidenh.client.capture.api.CaptureItemGuiFactory;
import com.hfstudio.guidenh.client.capture.api.CaptureNavigationOwner;
import com.hfstudio.guidenh.client.capture.api.CaptureNavigationState;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.client.capture.ui.CaptureBookPreviewScreen;

public class ItemGuiBatchProvider implements CaptureBatchProvider {

    private final Map<String, CaptureItemGuiFactory> factories = new LinkedHashMap<>();

    public ItemGuiBatchProvider() {
        register(
            new CaptureItemGuiFactory(
                "book",
                "Book",
                stack -> stack.getItem() == Items.written_book || stack.getItem() == Items.writable_book,
                context -> new CaptureBookPreviewScreen(context.stack())));
    }

    public void register(CaptureItemGuiFactory factory) {
        if (factories.putIfAbsent(factory.id(), factory) != null)
            throw new IllegalArgumentException("Duplicate item GUI factory: " + factory.id());
    }

    @Override
    public String id() {
        return "item_guis";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return Minecraft.getMinecraft().thePlayer != null;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.REACHABLE)) return List.of();
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean safe = inventoryContext(
            minecraft,
            context.capture()
                .screen())
            && minecraft.thePlayer.inventory.getItemStack() == null
            && (!(context.capture()
                .screen() instanceof GuiContainer) || context.capture()
                    .screen() instanceof GuiInventory
                || context.capture()
                    .screen() instanceof CaptureNavigationOwner
                || context.capture()
                    .screen() instanceof GuiContainerCreative);
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (int slot = 0; slot < minecraft.thePlayer.inventory.getSizeInventory(); slot++) {
            ItemStack stack = minecraft.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null) continue;
            for (CaptureItemGuiFactory factory : factories.values()) {
                if (!factory.supports()
                    .test(stack)) continue;
                targets.add(
                    new CaptureTargetDescriptor(
                        "item:" + factory.id() + ":" + slot,
                        id(),
                        CaptureBatchOptions.REACHABLE,
                        factory.label() + ": " + stack.getDisplayName(),
                        safe ? "ready" : "unavailable",
                        safe ? "" : "Use the player inventory before opening item previews",
                        Map.of("factory", factory.id(), "slot", Integer.toString(slot), "page", "0")));
            }
        }
        return List.copyOf(targets);
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new ItemNavigator(
            context.capture()
                .screen(),
            Map.copyOf(factories),
            options.scopes()
                .contains(CaptureBatchOptions.PAGES));
    }

    private static boolean inventoryContext(Minecraft minecraft, GuiScreen origin) {
        return minecraft.thePlayer != null
            && (minecraft.thePlayer.openContainer == minecraft.thePlayer.inventoryContainer
                || origin instanceof GuiContainerCreative creative
                    && minecraft.thePlayer.openContainer == creative.inventorySlots);
    }

    public static class ItemNavigator implements CaptureBatchNavigator {

        private final GuiScreen origin;
        private final Map<String, CaptureItemGuiFactory> factories;
        private final boolean pages;
        private GuiScreen selected;
        private String factoryId;
        private int slot = -1;
        private int page;
        private final CaptureNavigationState originState;

        public ItemNavigator(GuiScreen origin, Map<String, CaptureItemGuiFactory> factories, boolean pages) {
            this.origin = origin;
            this.factories = factories;
            this.pages = pages;
            originState = origin instanceof CaptureNavigationOwner owner ? owner.preserveCaptureNavigation() : null;
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            Minecraft minecraft = Minecraft.getMinecraft();
            int nextSlot = Integer.parseInt(
                target.data()
                    .get("slot"));
            String nextFactory = target.data()
                .get("factory");
            page = Integer.parseInt(
                target.data()
                    .get("page"));
            if (nextSlot != slot || !nextFactory.equals(factoryId)) {
                if (!inventoryContext(minecraft, origin) || minecraft.thePlayer.inventory.getItemStack() != null)
                    throw new IllegalStateException("Close the server container before opening an item preview");
                ItemStack stack = minecraft.thePlayer.inventory.getStackInSlot(nextSlot);
                CaptureItemGuiFactory factory = factories.get(nextFactory);
                if (stack == null || factory == null
                    || !factory.supports()
                        .test(stack))
                    throw new IllegalStateException("The catalog item changed");
                selected = factory.create()
                    .apply(new CaptureItemGuiContext(origin, nextSlot, stack));
                if (minecraft.currentScreen == origin && originState != null) originState.depart();
                minecraft.displayGuiScreen(selected);
                if (minecraft.currentScreen != selected)
                    throw new IllegalStateException("The item preview did not open");
                slot = nextSlot;
                factoryId = nextFactory;
            }
            if (selected instanceof CaptureBookPreviewScreen book) book.page(page);
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == selected && (!(selected instanceof CaptureBookPreviewScreen book) || book.page() == page);
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for the item preview");
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
            CaptureBatchContext context) {
            if (!pages || page != 0 || !(selected instanceof CaptureBookPreviewScreen book)) return List.of();
            List<CaptureTargetDescriptor> targets = new ArrayList<>();
            for (int next = 1; next < book.pageCount(); next++) targets.add(
                new CaptureTargetDescriptor(
                    target.id() + "/page:" + next,
                    target.providerId(),
                    target.scope(),
                    target.label() + " [" + next + "]",
                    "ready",
                    "",
                    Map.of("factory", factoryId, "slot", Integer.toString(slot), "page", Integer.toString(next))));
            return List.copyOf(targets);
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
            if (selected != null && Minecraft.getMinecraft().currentScreen != origin) {
                if (originState != null) originState.returning();
                Minecraft.getMinecraft()
                    .displayGuiScreen(origin);
            }
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = selected == null || context.capture()
                .screen() == origin;
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for the original screen");
        }

        @Override
        public void close() {
            if (originState != null) originState.close();
        }
    }
}
