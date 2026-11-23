package com.hfstudio.guidenh.integration.thaumcraft.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.integration.api.binding.BindingState;
import com.hfstudio.guidenh.integration.api.binding.BindingViewContext;
import com.hfstudio.guidenh.integration.thaumcraft.ThaumcraftResearchCatalog;
import com.hfstudio.guidenh.integration.thaumcraft.ThaumcraftResearchVisibility;
import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchBrowser;
import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchRecipe;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;

public class TcPageBatchProvider implements CaptureBatchProvider {

    private final ThaumcraftResearchCatalog catalog = new ThaumcraftResearchCatalog();
    private final ThaumcraftResearchVisibility visibility = new ThaumcraftResearchVisibility(catalog);

    @Override
    public String id() {
        return "thaumcraft_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture().screen() instanceof GuiResearchBrowser
            || context.capture().screen() instanceof GuiResearchRecipe;
    }

    private static BindingViewContext view() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return new BindingViewContext(player.getUniqueID(), player.getCommandSenderName(),
            Minecraft.getMinecraft().gameSettings.language, false, false, false);
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes().contains(CaptureBatchOptions.PAGES)) return List.of();
        if (Minecraft.getMinecraft().thePlayer == null) return List.of(new CaptureTargetDescriptor(
            "tc:unavailable", id(), CaptureBatchOptions.PAGES, I18n.format("guidenh.capture.thaumcraft.research"),
            "unavailable", "A player is required to browse Thaumcraft research", Map.of()));
        if (context.capture().screen() instanceof GuiResearchRecipe recipe) return spreads(recipe, true);
        BindingViewContext view = view();
        Set<String> progress = visibility.progress(view);
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (String category : ResearchCategories.researchCategories.keySet()) {
            if (category.equals("ELDRITCH") && !progress.contains("ELDRITCHMINOR")) continue;
            targets.add(new CaptureTargetDescriptor("tc:category:" + category, id(), CaptureBatchOptions.PAGES,
                ResearchCategories.getCategoryName(category), "ready", "", Map.of("kind", "category", "category", category)));
        }
        for (ResearchItem research : catalog.entries().values()) {
            BindingState state = visibility.state(research, view, progress);
            if (state.equals(BindingState.HIDDEN)) continue;
            boolean readable = state.equals(BindingState.COMPLETED) && research.getPages() != null
                && research.getPages().length > 0;
            targets.add(new CaptureTargetDescriptor("tc:research:" + research.key, id(), CaptureBatchOptions.PAGES,
                research.getName(), readable ? "ready" : "unavailable", readable ? ""
                    : state.equals(BindingState.COMPLETED) ? "This research has no readable pages"
                        : "Complete this research before opening its pages",
                Map.of("kind", "research", "category", research.category, "research", research.key, "page", "0")));
        }
        return List.copyOf(targets);
    }

    private static List<CaptureTargetDescriptor> spreads(GuiResearchRecipe screen, boolean includeFirst) {
        AccessorGuiResearchRecipe access = (AccessorGuiResearchRecipe) screen;
        ResearchItem research = access.guidenh$getResearch();
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (int page = includeFirst ? 0 : 2; page < access.guidenh$getCapturePageCount(); page += 2) {
            targets.add(new CaptureTargetDescriptor("tc:spread:" + research.key + ":" + page, "thaumcraft_pages",
                CaptureBatchOptions.PAGES, I18n.format("guidenh.capture.thaumcraft.spread", research.getName(), page / 2 + 1),
                "ready", "", Map.of("kind", "spread", "category", research.category, "research", research.key,
                    "page", Integer.toString(page), "pages", Integer.toString(access.guidenh$getCapturePageCount()))));
        }
        return List.copyOf(targets);
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new TcNavigator(context.capture().screen(), catalog, visibility, options.maxTargets());
    }

    public static class TcNavigator implements CaptureBatchNavigator {

        private final GuiScreen origin;
        private final TcCaptureNavigationState original;
        private final ThaumcraftResearchCatalog catalog;
        private final ThaumcraftResearchVisibility visibility;
        private final int targetLimit;
        private GuiScreen active;
        private CaptureTargetDescriptor selected;
        private TcCaptureMap map;
        private int page;

        public TcNavigator(GuiScreen origin, ThaumcraftResearchCatalog catalog, ThaumcraftResearchVisibility visibility,
            int targetLimit) {
            this.origin = origin;
            this.catalog = catalog;
            this.visibility = visibility;
            this.targetLimit = targetLimit;
            original = new TcCaptureNavigationState(origin);
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            if (Minecraft.getMinecraft().thePlayer == null) throw new IllegalStateException("The research player is no longer available");
            if (target.data().get("kind").equals("map_slice")) {
                if (!(active instanceof GuiResearchBrowser browser) || map == null
                    || Minecraft.getMinecraft().currentScreen != active
                    || !map.matches(browser)
                    || !map.category().equals(target.data().get("category")))
                    throw new IllegalStateException("The selected research map is no longer open");
                map.position(browser, Double.parseDouble(target.data().get("x")), Double.parseDouble(target.data().get("y")));
                selected = target;
                return;
            }
            map = null;
            if (target.data().get("kind").equals("category")) {
                String category = target.data().get("category");
                if (!ResearchCategories.researchCategories.containsKey(category)
                    || category.equals("ELDRITCH") && !visibility.progress(view()).contains("ELDRITCHMINOR"))
                    throw new IllegalStateException("The research category is no longer accessible");
                AccessorGuiResearchBrowser.guidenh$setSelectedCategory(category);
                active = new GuiResearchBrowser(original.mapX(), original.mapY());
                page = 0;
            } else {
                catalog.entries();
                ResearchItem research = catalog.get(target.data().get("research"));
                boolean current = origin instanceof GuiResearchRecipe recipe
                    && ((AccessorGuiResearchRecipe) recipe).guidenh$getResearch() == research;
                if (research == null || !current && !visibility.progress(view()).contains(research.key))
                    throw new IllegalStateException("The selected research is no longer readable");
                page = Integer.parseInt(target.data().get("page"));
                double x = original.mapX();
                double y = original.mapY();
                if (origin instanceof GuiResearchRecipe recipe) {
                    x = ((AccessorGuiResearchRecipe) recipe).guidenh$getCaptureMapX();
                    y = ((AccessorGuiResearchRecipe) recipe).guidenh$getCaptureMapY();
                }
                GuiResearchRecipe recipe = new GuiResearchRecipe(research, page, x, y);
                if (page < 0 || page % 2 != 0 || page >= ((AccessorGuiResearchRecipe) recipe).guidenh$getCapturePageCount())
                    throw new IllegalStateException("The selected research spread is no longer available");
                active = recipe;
            }
            selected = target;
            Minecraft.getMinecraft().displayGuiScreen(active);
            if (Minecraft.getMinecraft().currentScreen != active)
                throw new IllegalStateException("The research GUI opening was cancelled or replaced");
            if (active instanceof GuiResearchBrowser browser) {
                BindingViewContext view = view();
                Set<String> progress = visibility.progress(view);
                List<ResearchItem> visible = catalog.entries().values().stream()
                    .filter(item -> item.category.equals(target.data().get("category")))
                    .filter(item -> !visibility.state(item, view, progress).equals(BindingState.HIDDEN)).toList();
                map = TcCaptureMap.capture(browser, visible);
                if (map != null) map.position(browser, map.minimumX(), map.minimumY());
            }
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = active != null && context.capture().screen() == active && active.width > 0 && active.height > 0;
            if (active instanceof GuiResearchRecipe recipe)
                ready &= ((AccessorGuiResearchRecipe) recipe).guidenh$getCapturePage() == page;
            if (active instanceof GuiResearchBrowser && selected != null)
                ready &= selected.data().get("category").equals(AccessorGuiResearchBrowser.guidenh$getCaptureCategory());
            return new CaptureBatchReadiness(ready, Integer.toUnsignedLong(System.identityHashCode(active)) * 31 + page,
                ready ? "" : "Waiting for the selected research page");
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            if (selected != null && selected.data().get("kind").equals("map_slice") && map != null)
                return List.of(map.viewport());
            return List.of(context.capture().viewport().bounds());
        }

        @Override
        public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
            return selected != null && selected.data().get("kind").equals("map_slice")
                && active instanceof GuiResearchBrowser browser && map != null ? map.geometry(browser) : null;
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target, CaptureBatchContext context) {
            if (target.data().get("kind").equals("research") && active instanceof GuiResearchRecipe recipe)
                return spreads(recipe, false);
            return target.data().get("kind").equals("category") && map != null ? map.slices(targetLimit) : List.of();
        }

        @Override
        public void restore() {
            original.prepareRestore();
            if (Minecraft.getMinecraft().currentScreen != origin) Minecraft.getMinecraft().displayGuiScreen(origin);
            original.restore(origin);
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture().screen() == origin && original.restored();
            return new CaptureBatchReadiness(ready, context.geometry().revision(), ready ? "" : "Waiting for the original research GUI");
        }
    }
}
