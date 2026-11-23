package com.hfstudio.guidenh.integration.nei.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiTextField;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.mixins.late.compat.nei.AccessorCaptureNeiRecipe;
import com.hfstudio.guidenh.mixins.late.compat.nei.AccessorCaptureNeiTask;
import com.hfstudio.guidenh.mixins.late.compat.nei.AccessorCaptureNeiText;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.RecipeSearchField;
import codechicken.nei.RestartableTask;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipePageManager;
import codechicken.nei.scroll.ScrollContainer;

public class NeiPageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "nei_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof GuiRecipe<?>;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        GuiRecipe<?> gui = (GuiRecipe<?>) context.capture()
            .screen();
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (int index = 0; index < gui.currenthandlers.size(); index++) {
            IRecipeHandler handler = gui.currenthandlers.get(index);
            targets.add(
                new CaptureTargetDescriptor(
                    "nei:handler:" + index,
                    id(),
                    CaptureBatchOptions.PAGES,
                    handler.getRecipeName(),
                    handler.numRecipes() > 0 ? "ready" : "unavailable",
                    handler.numRecipes() > 0 ? "" : "This handler has no recipes",
                    Map.of(
                        "handler",
                        handler.getHandlerId(),
                        "ordinal",
                        Integer.toString(ordinal(gui, index)),
                        "page",
                        "0",
                        "scrollX",
                        "0",
                        "scrollY",
                        "0",
                        "pages",
                        "true")));
        }
        return List.copyOf(targets);
    }

    private static int ordinal(GuiRecipe<?> gui, int index) {
        String id = gui.currenthandlers.get(index)
            .getHandlerId();
        int ordinal = 0;
        for (int prior = 0; prior < index; prior++) if (gui.currenthandlers.get(prior)
            .getHandlerId()
            .equals(id)) ordinal++;
        return ordinal;
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new RecipeNavigator(
            (GuiRecipe<?>) context.capture()
                .screen());
    }

    private static boolean filterReady() {
        RestartableTask task = AccessorCaptureNeiRecipe.guidenh$getCaptureFilterTask();
        synchronized (task) {
            Thread thread = ((AccessorCaptureNeiTask) task).guidenh$getCaptureThread();
            return thread == null || !thread.isAlive();
        }
    }

    public static class RecipeNavigator implements CaptureBatchNavigator {

        private final GuiRecipe<?> gui;
        private final AccessorCaptureNeiRecipe access;
        private final String originalHandler;
        private final int originalOrdinal;
        private final int originalPage;
        private final int originalX;
        private final int originalY;
        private final String originalSearch;
        private final boolean originalSearchVisible;
        private final int originalSearchToggle;
        private final boolean originalFocus;
        private final int originalCursor;
        private final int originalSelection;
        private CaptureTargetDescriptor selected;
        private int page;
        private int x;
        private int y;

        public RecipeNavigator(GuiRecipe<?> gui) {
            this.gui = gui;
            access = (AccessorCaptureNeiRecipe) gui;
            originalHandler = gui.getHandler()
                .getHandlerId();
            originalOrdinal = ordinal(gui, gui.recipetype);
            originalPage = access.guidenh$getCapturePages()
                .getCurrentPageIndex();
            originalX = access.guidenh$getCaptureContainer()
                .getHorizontalScrollOffset();
            originalY = access.guidenh$getCaptureContainer()
                .getVerticalScrollOffset();
            RecipeSearchField search = AccessorCaptureNeiRecipe.guidenh$getCaptureSearch();
            originalSearch = search.text();
            originalSearchVisible = search.isVisible();
            originalSearchToggle = AccessorCaptureNeiRecipe.guidenh$getCaptureSearchToggle().state;
            GuiTextField input = ((AccessorCaptureNeiText) search).guidenh$getCaptureTextInput();
            originalFocus = input.isFocused();
            originalCursor = input.getCursorPosition();
            originalSelection = input.getSelectionEnd();
        }

        private void handler(String id, int ordinal) {
            int matching = 0;
            for (int index = 0; index < gui.currenthandlers.size(); index++) {
                if (!gui.currenthandlers.get(index)
                    .getHandlerId()
                    .equals(id)) continue;
                if (matching++ != ordinal) continue;
                if (gui.recipetype != index) access.guidenh$selectCaptureHandler(index);
                if (id.equals(originalHandler) && ordinal == originalOrdinal) restoreSearch();
                return;
            }
            throw new IllegalStateException("The selected NEI handler is no longer available");
        }

        private void restoreSearch() {
            RecipeSearchField search = AccessorCaptureNeiRecipe.guidenh$getCaptureSearch();
            if (!search.text()
                .equals(originalSearch)) search.setText(originalSearch);
            search.setVisible(originalSearchVisible);
            AccessorCaptureNeiRecipe.guidenh$getCaptureSearchToggle().state = originalSearchToggle;
            GuiTextField input = ((AccessorCaptureNeiText) search).guidenh$getCaptureTextInput();
            input.setFocused(originalFocus);
            input.setCursorPosition(originalCursor);
            input.setSelectionPos(originalSelection);
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selected = target;
            page = Integer.parseInt(
                target.data()
                    .get("page"));
            x = Integer.parseInt(
                target.data()
                    .get("scrollX"));
            y = Integer.parseInt(
                target.data()
                    .get("scrollY"));
            handler(
                target.data()
                    .get("handler"),
                Integer.parseInt(
                    target.data()
                        .get("ordinal")));
            move(page, x, y);
        }

        private boolean move(int page, int x, int y) {
            if (!filterReady()) return false;
            RecipePageManager pages = access.guidenh$getCapturePages();
            boolean changed = pages.rebuildPages();
            if (page < 0 || page >= pages.getNumPages()) return false;
            if (pages.getCurrentPageIndex() != page) {
                pages.changePage(page - pages.getCurrentPageIndex());
                changed = true;
            }
            if (changed) access.guidenh$refreshCaptureContainer();
            ScrollContainer container = access.guidenh$getCaptureContainer();
            boolean matches = container.getHorizontalScrollOffset() == x && container.getVerticalScrollOffset() == y;
            container.setHorizontalScrollOffset(x);
            container.setVerticalScrollOffset(y);
            return !changed && matches
                && pages.getCurrentPageIndex() == page
                && container.getHorizontalScrollOffset() == x
                && container.getVerticalScrollOffset() == y;
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            RecipePageManager pages = access.guidenh$getCapturePages();
            boolean samePage = pages.getCurrentPageIndex() == page;
            boolean ready = context.capture()
                .screen() == gui && move(page, x, y)
                && samePage;
            long revision = context.geometry()
                .revision() * 31 + pages.getNumRecipes() * 17L
                + pages.getCurrentPageIndex();
            return new CaptureBatchReadiness(ready, revision, ready ? "" : "Waiting for NEI filtering and page layout");
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
            CaptureBatchContext context) {
            if (target.data()
                .get("pages")
                .equals("slice")) return List.of();
            List<CaptureTargetDescriptor> targets = new ArrayList<>();
            if (target.data()
                .get("pages")
                .equals("true")) {
                for (int next = 1; next < access.guidenh$getCapturePages()
                    .getNumPages(); next++) {
                    targets.add(child(target, next, 0, 0, "false"));
                }
            }
            ScrollContainer container = access.guidenh$getCaptureContainer();
            int stepX = Math.max(1, container.getVisibleWidth());
            int stepY = Math.max(1, container.getVisibleHeight());
            int maxX = Math.max(0, container.getActualWidth() - stepX);
            int maxY = Math.max(0, container.getActualHeight() - stepY);
            for (int offsetY = 0; offsetY <= maxY; offsetY += stepY) {
                for (int offsetX = 0; offsetX <= maxX; offsetX += stepX) {
                    if (offsetX > 0 || offsetY > 0) targets.add(child(target, page, offsetX, offsetY, "slice"));
                }
                if (maxX > 0 && maxX % stepX != 0) targets.add(child(target, page, maxX, offsetY, "slice"));
            }
            if (maxY > 0 && maxY % stepY != 0) {
                for (int offsetX = 0; offsetX <= maxX; offsetX += stepX)
                    targets.add(child(target, page, offsetX, maxY, "slice"));
                if (maxX > 0 && maxX % stepX != 0) targets.add(child(target, page, maxX, maxY, "slice"));
            }
            return targets;
        }

        private CaptureTargetDescriptor child(CaptureTargetDescriptor parent, int page, int x, int y, String expand) {
            return new CaptureTargetDescriptor(
                "nei:" + parent.data()
                    .get("handler")
                    + ":"
                    + parent.data()
                        .get("ordinal")
                    + "/page:"
                    + page
                    + "/scroll:"
                    + x
                    + ":"
                    + y,
                parent.providerId(),
                parent.scope(),
                parent.label() + " · " + (page + 1) + " [" + x + "," + y + "]",
                "ready",
                "",
                Map.of(
                    "handler",
                    parent.data()
                        .get("handler"),
                    "ordinal",
                    parent.data()
                        .get("ordinal"),
                    "page",
                    Integer.toString(page),
                    "scrollX",
                    Integer.toString(x),
                    "scrollY",
                    Integer.toString(y),
                    "pages",
                    expand));
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            if (!selected.data()
                .get("pages")
                .equals("slice"))
                return List.of(
                    context.capture()
                        .viewport()
                        .bounds());
            Rectangle4i bounds = access.guidenh$getCaptureContainer()
                .boundsInside();
            return List.of(new CaptureRect(bounds.x, bounds.y, bounds.w, bounds.h));
        }

        @Override
        public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
            ScrollContainer container = access.guidenh$getCaptureContainer();
            Rectangle4i bounds = container.boundsInside();
            return new CaptureScrollGeometry(
                selected.data()
                    .get("handler") + "/"
                    + selected.data()
                        .get("ordinal")
                    + "/"
                    + page,
                new CaptureRect(bounds.x, bounds.y, container.getVisibleWidth(), container.getVisibleHeight()),
                x,
                y,
                Math.max(container.getActualWidth(), container.getVisibleWidth()),
                Math.max(container.getActualHeight(), container.getVisibleHeight()));
        }

        @Override
        public void restore() {
            handler(originalHandler, originalOrdinal);
            move(originalPage, originalX, originalY);
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            RecipePageManager pages = access.guidenh$getCapturePages();
            boolean samePage = pages.getCurrentPageIndex() == originalPage;
            boolean ready = context.capture()
                .screen() == gui && gui.getHandler()
                    .getHandlerId()
                    .equals(originalHandler)
                && move(originalPage, originalX, originalY)
                && samePage;
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the original NEI filter, page, and scroll offsets");
        }
    }
}
