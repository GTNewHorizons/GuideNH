package com.hfstudio.guidenh.integration.nei.capture;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.mixins.late.compat.nei.AccessorCaptureNeiLayout;
import com.hfstudio.guidenh.mixins.late.compat.nei.AccessorCaptureNeiRecipe;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.ItemsGrid;
import codechicken.nei.ItemsGrid.ItemsGridSlot;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PanelWidget;
import codechicken.nei.Widget;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.scroll.ScrollContainer;

public class NeiCaptureGeometryProvider implements CaptureGeometryProvider {

    @Override
    public String id() {
        return "nei";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return context.screen() instanceof GuiContainer && !NEIClientConfig.isHidden() && NEIClientConfig.isEnabled();
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        List<CaptureTarget> targets = new ArrayList<>();
        int index = 0;
        for (Widget widget : AccessorCaptureNeiLayout.guidenh$getCaptureWidgets()) {
            if (widget.w <= 0 || widget.h <= 0) continue;
            String id = "nei:widget:" + index++;
            CaptureRect bounds = new CaptureRect(widget.x, widget.y, widget.w, widget.h);
            targets.add(
                target(
                    widget,
                    id,
                    "minecraft:screen",
                    widget instanceof PanelWidget ? CaptureTarget.WINDOW : CaptureTarget.COMPONENT,
                    bounds,
                    context.viewport()
                        .bounds(),
                    widget.z,
                    20));
            if (widget instanceof PanelWidget<?>panel) {
                ItemsGrid<?, ?> grid = panel.getGrid();
                for (ItemsGridSlot slot : grid.getMask()) {
                    Rectangle4i rectangle = grid.getSlotRect(slot.slotIndex);
                    CaptureRect cell = new CaptureRect(rectangle.x, rectangle.y, rectangle.w, rectangle.h);
                    targets.add(
                        target(
                            widget,
                            id + "/slot:" + slot.itemIndex,
                            id,
                            CaptureTarget.SLOT,
                            cell,
                            bounds.intersect(
                                context.viewport()
                                    .bounds()),
                            widget.z,
                            40));
                }
            }
        }
        if (context.screen() instanceof GuiRecipe<?>recipe) {
            ScrollContainer container = ((AccessorCaptureNeiRecipe) recipe).guidenh$getCaptureContainer();
            Rectangle4i rectangle = container.boundsInside();
            CaptureRect bounds = new CaptureRect(rectangle.x, rectangle.y, rectangle.w, rectangle.h);
            targets.add(
                target(
                    container,
                    "nei:recipes",
                    "minecraft:container",
                    CaptureTarget.COMPONENT,
                    bounds,
                    context.viewport()
                        .bounds(),
                    container.z,
                    25));
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }

    private static CaptureTarget target(Widget widget, String id, String parent, String kind, CaptureRect bounds,
        CaptureRect clip, int zOrder, int priority) {
        return new CaptureTarget(
            id,
            parent,
            kind,
            widget.getClass()
                .getName(),
            widget.getClass()
                .getSimpleName(),
            bounds,
            bounds.intersect(clip),
            zOrder + 2,
            priority,
            Integer.toUnsignedLong(System.identityHashCode(widget)));
    }
}
