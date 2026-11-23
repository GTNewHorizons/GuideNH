package com.hfstudio.guidenh.integration.thaumcraft.capture;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchBrowser;

import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;

public class TcCaptureNavigationState {

    public record BrowserPosition(double x, double y, double previousX, double previousY,
        double targetX, double targetY) {

        public static BrowserPosition capture(GuiResearchBrowser screen) {
            AccessorGuiResearchBrowser access = (AccessorGuiResearchBrowser) screen;
            return new BrowserPosition(access.guidenh$getCaptureMapX(), access.guidenh$getCaptureMapY(),
                access.guidenh$getCapturePreviousX(), access.guidenh$getCapturePreviousY(),
                access.guidenh$getCaptureTargetX(), access.guidenh$getCaptureTargetY());
        }

        public void restore(GuiResearchBrowser screen) {
            AccessorGuiResearchBrowser access = (AccessorGuiResearchBrowser) screen;
            access.guidenh$setCaptureMapX(x);
            access.guidenh$setCaptureMapY(y);
            access.guidenh$setCapturePreviousX(previousX);
            access.guidenh$setCapturePreviousY(previousY);
            access.guidenh$setCaptureTargetX(targetX);
            access.guidenh$setCaptureTargetY(targetY);
        }
    }

    private final String category;
    private final int lastX;
    private final int lastY;
    private final LinkedList<Object[]> history;
    private final List<Object[]> historyEntries;
    private final BrowserPosition browserPosition;

    public TcCaptureNavigationState(GuiScreen origin) {
        category = AccessorGuiResearchBrowser.guidenh$getCaptureCategory();
        lastX = GuiResearchBrowser.lastX;
        lastY = GuiResearchBrowser.lastY;
        history = GuiResearchRecipe.history;
        historyEntries = new ArrayList<>(history);
        browserPosition = origin instanceof GuiResearchBrowser browser ? BrowserPosition.capture(browser) : null;
    }

    public double mapX() {
        return browserPosition == null ? 0 : browserPosition.x();
    }

    public double mapY() {
        return browserPosition == null ? 0 : browserPosition.y();
    }

    public void prepareRestore() {
        AccessorGuiResearchBrowser.guidenh$setSelectedCategory(category);
    }

    public void restore(GuiScreen origin) {
        prepareRestore();
        if (origin instanceof GuiResearchBrowser browser) {
            browser.updateResearch();
            browserPosition.restore(browser);
        }
        GuiResearchBrowser.lastX = lastX;
        GuiResearchBrowser.lastY = lastY;
        GuiResearchRecipe.history = history;
        history.clear();
        history.addAll(historyEntries);
    }

    public boolean restored() {
        return Objects.equals(category, AccessorGuiResearchBrowser.guidenh$getCaptureCategory())
            && GuiResearchBrowser.lastX == lastX && GuiResearchBrowser.lastY == lastY
            && GuiResearchRecipe.history == history && history.equals(historyEntries);
    }
}
