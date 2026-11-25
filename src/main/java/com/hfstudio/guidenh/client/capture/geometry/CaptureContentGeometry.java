package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class CaptureContentGeometry {

    public static CaptureGeometry visible(CaptureGeometry geometry, CaptureContentOptions content) {
        List<CaptureTarget> targets = geometry.targets()
            .stream()
            .filter(target -> visible(target, content))
            .toList();
        return new CaptureGeometry(targets.hashCode(), geometry.viewport(), targets);
    }

    public static boolean visible(CaptureTarget target, CaptureContentOptions content) {
        if (target.kind()
            .equals(CaptureTarget.SCREEN)) return true;
        if (target.playerInventory() && !content.includePlayerInventory()) return false;
        if (target.contentLayer()
            .equals(CaptureTarget.HUD_LAYER)) return content.includeHud();
        if (!content.includeGui()) return false;
        return switch (target.contentLayer()) {
            case CaptureTarget.NEI_LAYER -> content.includeNei();
            case CaptureTarget.TOOLTIP_LAYER -> content.includeTooltips();
            default -> true;
        };
    }

    public static List<CaptureRect> inventoryMask(CaptureGeometry geometry, CaptureContentOptions content) {
        if (content.includePlayerInventory() || !content.includeGui()) return List.of();
        Map<Integer, Map<String, CaptureRect>> layers = new LinkedHashMap<>();
        for (CaptureTarget target : geometry.targets()) {
            if (!target.playerInventory() || target.visibleBounds()
                .isEmpty()) continue;
            layers.computeIfAbsent(target.zOrder(), ignored -> new LinkedHashMap<>())
                .merge(target.parentId(), target.visibleBounds(), CaptureRect::union);
        }
        if (layers.isEmpty()) return List.of();
        List<CaptureTarget> retained = geometry.targets()
            .stream()
            .filter(target -> !target.playerInventory() && visible(target, content))
            .filter(
                target -> !target.visibleBounds()
                    .isEmpty()
                    && !target.kind()
                        .equals(CaptureTarget.SCREEN))
            .toList();
        List<CaptureRect> masks = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, CaptureRect>> layer : layers.entrySet()) {
            for (CaptureRect bounds : layer.getValue()
                .values()) {
                masks.addAll(inventoryMask(bounds, layer.getKey(), retained));
            }
        }
        return List.copyOf(masks);
    }

    private static List<CaptureRect> inventoryMask(CaptureRect bounds, int layer, List<CaptureTarget> retained) {
        List<CaptureRect> masks = List.of(bounds);
        for (CaptureTarget target : retained) {
            if (!target.kind()
                .equals(CaptureTarget.TOOLTIP)
                && (target.zOrder() < layer || target.zOrder() == layer && target.kind()
                    .equals(CaptureTarget.WINDOW)))
                continue;
            List<CaptureRect> remaining = new ArrayList<>(masks.size());
            for (CaptureRect mask : masks) remaining.addAll(mask.subtract(target.visibleBounds()));
            masks = remaining;
            if (masks.isEmpty()) break;
        }
        return masks;
    }
}
