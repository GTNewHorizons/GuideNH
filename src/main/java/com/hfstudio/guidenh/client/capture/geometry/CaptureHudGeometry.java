package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiIngame;

import com.hfstudio.guidenh.client.capture.api.CaptureGeometryCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureHudGeometry {

    private CaptureHudGeometry() {}

    public static List<CaptureTarget> hotbar(CaptureViewport viewport, long ownerIdentity) {
        int center = viewport.logicalWidth() / 2;
        int bottom = viewport.logicalHeight();
        List<CaptureTarget> targets = new ArrayList<>(10);
        // Include the selected-slot outline, which extends one pixel beyond the native bar texture.
        CaptureRect bar = new CaptureRect(center - 92, bottom - 23, 184, 23);
        targets.add(
            target(
                "minecraft:hotbar",
                "minecraft:screen",
                CaptureTarget.COMPONENT,
                "Hotbar",
                bar,
                viewport,
                ownerIdentity,
                25));
        for (int slot = 0; slot < 9; slot++) {
            CaptureRect bounds = new CaptureRect(center - 89 + slot * 20, bottom - 20, 18, 18);
            targets.add(
                target(
                    "minecraft:hotbar:slot:" + slot,
                    "minecraft:hotbar",
                    CaptureTarget.SLOT,
                    "Hotbar slot " + slot,
                    bounds,
                    viewport,
                    ownerIdentity,
                    35));
        }
        return List.copyOf(targets);
    }

    private static CaptureTarget target(String id, String parent, String kind, String label, CaptureRect bounds,
        CaptureViewport viewport, long ownerIdentity, int priority) {
        return new CaptureTarget(
            id,
            parent,
            kind,
            GuiIngame.class.getName(),
            label,
            bounds,
            bounds.intersect(viewport.bounds()),
            0,
            priority,
            ownerIdentity).withContent(CaptureTarget.HUD_LAYER, true)
                .withProvider(CaptureGeometryCatalog.VANILLA_HUD);
    }
}
