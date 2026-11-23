package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorGuiCaptureContainer;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorGuiCaptureScreen;

public class VanillaCaptureGeometryProvider implements CaptureGeometryProvider {

    @Override
    public String id() {
        return "minecraft";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return true;
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        GuiScreen screen = context.screen();
        CaptureRect viewport = context.viewport()
            .bounds();
        List<CaptureTarget> targets = new ArrayList<>();
        targets.add(
            target(
                "screen",
                "",
                CaptureTarget.SCREEN,
                screen,
                screen.getClass()
                    .getSimpleName(),
                viewport,
                viewport,
                -100));
        String parent = "screen";
        if (screen instanceof GuiContainer container) {
            AccessorGuiCaptureContainer access = (AccessorGuiCaptureContainer) container;
            CaptureRect window = new CaptureRect(
                access.guidenh$getCaptureGuiLeft(),
                access.guidenh$getCaptureGuiTop(),
                access.guidenh$getCaptureGuiWidth(),
                access.guidenh$getCaptureGuiHeight());
            targets.add(
                target(
                    "window",
                    "screen",
                    CaptureTarget.WINDOW,
                    container,
                    container.getClass()
                        .getSimpleName(),
                    window,
                    viewport,
                    -10));
            parent = "window";
            for (Slot slot : container.inventorySlots.inventorySlots) {
                CaptureRect bounds = new CaptureRect(
                    access.guidenh$getCaptureGuiLeft() + slot.xDisplayPosition - 1,
                    access.guidenh$getCaptureGuiTop() + slot.yDisplayPosition - 1,
                    18,
                    18);
                targets.add(
                    target(
                        "slot:" + slot.slotNumber,
                        parent,
                        CaptureTarget.SLOT,
                        slot,
                        "Slot " + slot.slotNumber,
                        bounds,
                        viewport,
                        20));
            }
        }
        for (GuiButton button : ((AccessorGuiCaptureScreen) screen).guidenh$getCaptureButtons()) {
            if (!button.visible || button.width <= 0 || button.height <= 0) continue;
            CaptureRect bounds = new CaptureRect(button.xPosition, button.yPosition, button.width, button.height);
            targets.add(
                target(
                    "button:" + button.id,
                    parent,
                    CaptureTarget.COMPONENT,
                    button,
                    button.displayString,
                    bounds,
                    viewport,
                    30));
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }

    private static CaptureTarget target(String id, String parent, String kind, Object owner, String label,
        CaptureRect bounds, CaptureRect viewport, int priority) {
        return new CaptureTarget(
            "minecraft:" + id,
            parent.isEmpty() ? "" : "minecraft:" + parent,
            kind,
            owner.getClass()
                .getName(),
            label,
            bounds,
            bounds.intersect(viewport),
            0,
            priority,
            Integer.toUnsignedLong(System.identityHashCode(owner)));
    }
}
