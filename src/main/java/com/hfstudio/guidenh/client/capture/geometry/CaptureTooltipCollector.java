package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureTooltipCollector {

    private final List<CaptureTarget> targets = new ArrayList<>();

    public void clear() {
        targets.clear();
    }

    public void record(CaptureRect bounds, CaptureViewport viewport) {
        CaptureRect visible = bounds.intersect(viewport.bounds());
        if (visible.isEmpty() || targets.stream()
            .anyMatch(
                target -> target.bounds()
                    .equals(bounds)))
            return;
        targets.add(
            new CaptureTarget(
                "tooltip:" + targets.size(),
                "minecraft:screen",
                CaptureTarget.TOOLTIP,
                CaptureTooltipCollector.class.getName(),
                "Tooltip",
                bounds,
                visible,
                Integer.MAX_VALUE,
                100).withContent(CaptureTarget.TOOLTIP_LAYER, false));
    }

    public CaptureGeometry merge(CaptureGeometry geometry) {
        List<CaptureTarget> combined = new ArrayList<>(geometry.targets());
        combined.addAll(targets);
        return new CaptureGeometry(combined.hashCode(), geometry.viewport(), combined);
    }

    public static CaptureRect vanillaBounds(List<String> lines, int mouseX, int mouseY, FontRenderer font,
        CaptureViewport viewport) {
        int width = lines.stream()
            .mapToInt(font::getStringWidth)
            .max()
            .orElse(0);
        int height = lines.size() <= 1 ? 8 : 10 * lines.size();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width > viewport.logicalWidth()) x -= 28 + width;
        if (y + height + 6 > viewport.logicalHeight()) y = viewport.logicalHeight() - height - 6;
        return new CaptureRect(x - 4, y - 4, width + 8, height + 8);
    }
}
