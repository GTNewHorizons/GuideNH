package com.hfstudio.guidenh.integration.thaumcraft.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.I18n;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchBrowser;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

public record TcCaptureMap(String category, CaptureRect viewport, double clipX, double clipY, double originX,
    double originY, double contentWidth, double contentHeight, double minimumX, double maximumX, double minimumY,
    double maximumY) {

    public static TcCaptureMap capture(GuiResearchBrowser screen, List<ResearchItem> visible) {
        if (visible.isEmpty()) return null;
        String category = AccessorGuiResearchBrowser.guidenh$getCaptureCategory();
        ResearchCategoryList list = ResearchCategories.getResearchList(category);
        CaptureRect raw = viewport(screen);
        CaptureRect viewport = raw.intersect(new CaptureRect(0, 0, screen.width, screen.height));
        if (viewport.isEmpty()) return null;
        double clipX = viewport.x() - raw.x();
        double clipY = viewport.y() - raw.y();
        double left = visible.stream()
            .mapToDouble(item -> item.displayColumn * 24d - 4)
            .min()
            .orElseThrow();
        double top = visible.stream()
            .mapToDouble(item -> item.displayRow * 24d - 4)
            .min()
            .orElseThrow();
        double right = visible.stream()
            .mapToDouble(item -> item.displayColumn * 24d + 28)
            .max()
            .orElseThrow();
        double bottom = visible.stream()
            .mapToDouble(item -> item.displayRow * 24d + 28)
            .max()
            .orElseThrow();
        double maximumX = list.maxDisplayColumn * 24d - 113;
        double maximumY = list.maxDisplayRow * 24d - 62;
        double lowerX = Math.min(list.minDisplayColumn * 24d - 85, maximumX);
        double lowerY = Math.min(list.minDisplayRow * 24d - 112, maximumY);
        double minimumX = Math.max(lowerX, Math.min(maximumX, left - clipX));
        double minimumY = Math.max(lowerY, Math.min(maximumY, top - clipY));
        maximumX = Math.max(minimumX, Math.min(maximumX, right - viewport.width() - clipX));
        maximumY = Math.max(minimumY, Math.min(maximumY, bottom - viewport.height() - clipY));
        double originX = Math.min(left, minimumX + clipX);
        double originY = Math.min(top, minimumY + clipY);
        double width = Math.max(right, minimumX + clipX + viewport.width()) - originX;
        double height = Math.max(bottom, minimumY + clipY + viewport.height()) - originY;
        return new TcCaptureMap(
            category,
            viewport,
            clipX,
            clipY,
            originX,
            originY,
            width,
            height,
            minimumX,
            maximumX,
            minimumY,
            maximumY);
    }

    private static CaptureRect viewport(GuiResearchBrowser screen) {
        AccessorGuiResearchBrowser access = (AccessorGuiResearchBrowser) screen;
        // The browser renders a 224 by 196 map inside its frame, offset by 16 and 17 pixels.
        return new CaptureRect(
            (screen.width - access.guidenh$getCapturePaneWidth()) / 2 + 16,
            (screen.height - access.guidenh$getCapturePaneHeight()) / 2 + 17,
            224,
            196);
    }

    public boolean matches(GuiResearchBrowser screen) {
        return viewport.equals(viewport(screen).intersect(new CaptureRect(0, 0, screen.width, screen.height)));
    }

    public void position(GuiResearchBrowser screen, double x, double y) {
        double left = Math.max(minimumX, Math.min(maximumX, x));
        double top = Math.max(minimumY, Math.min(maximumY, y));
        new TcCaptureNavigationState.BrowserPosition(left, top, left, top, left, top).restore(screen);
    }

    public CaptureScrollGeometry geometry(GuiResearchBrowser screen) {
        AccessorGuiResearchBrowser access = (AccessorGuiResearchBrowser) screen;
        return new CaptureScrollGeometry(
            "tc-map/" + category,
            viewport,
            access.guidenh$getCaptureMapX() + clipX - originX,
            access.guidenh$getCaptureMapY() + clipY - originY,
            contentWidth,
            contentHeight);
    }

    public List<CaptureTargetDescriptor> slices(int limit) {
        if (limit < 1) throw new IllegalArgumentException("The research map target limit must be positive");
        long columns = (long) Math.ceil((maximumX - minimumX) / viewport.width()) + 1;
        long rows = (long) Math.ceil((maximumY - minimumY) / viewport.height()) + 1;
        long total = Math.multiplyExact(columns, rows);
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        long count = Math.min(total, limit);
        for (long index = 0; index < count; index++) {
            double x = Math.min(maximumX, minimumX + index % columns * viewport.width());
            double y = Math.min(maximumY, minimumY + index / columns * viewport.height());
            targets.add(
                new CaptureTargetDescriptor(
                    "tc:map:" + category + ":" + index,
                    "thaumcraft_pages",
                    CaptureBatchOptions.PAGES,
                    I18n.format("guidenh.capture.thaumcraft.map_slice", category, index + 1),
                    "ready",
                    "",
                    Map.of(
                        "kind",
                        "map_slice",
                        "category",
                        category,
                        "x",
                        Double.toString(x),
                        "y",
                        Double.toString(y),
                        "mapSlices",
                        Long.toString(total))));
        }
        if (total > count) targets.add(
            new CaptureTargetDescriptor(
                "tc:map:" + category + ":remaining",
                "thaumcraft_pages",
                CaptureBatchOptions.PAGES,
                I18n.format("guidenh.capture.thaumcraft.map_slice", category, count + 1),
                "unavailable",
                "The research map exceeds the configured target limit",
                Map.of("kind", "map_limit", "category", category, "remaining", Long.toString(total - count))));
        return List.copyOf(targets);
    }
}
