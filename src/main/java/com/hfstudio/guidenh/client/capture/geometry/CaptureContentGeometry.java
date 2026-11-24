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
        Map<String, CaptureRect> groups = new LinkedHashMap<>();
        for (CaptureTarget target : geometry.targets()) {
            if (!target.playerInventory() || target.visibleBounds()
                .isEmpty()) continue;
            groups.merge(target.parentId() + ":" + target.zOrder(), target.visibleBounds(), CaptureRect::union);
        }
        List<CaptureRect> masks = new ArrayList<>(groups.values());
        for (CaptureTarget target : geometry.targets()) {
            if (target.playerInventory() || !target.kind()
                .equals(CaptureTarget.SLOT)
                && !target.kind()
                    .equals(CaptureTarget.TOOLTIP))
                continue;
            masks = masks.stream()
                .flatMap(
                    mask -> mask.subtract(target.visibleBounds())
                        .stream())
                .toList();
        }
        return List.copyOf(masks);
    }
}
