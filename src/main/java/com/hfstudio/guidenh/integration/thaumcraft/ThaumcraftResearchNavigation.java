package com.hfstudio.guidenh.integration.thaumcraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.api.binding.BindingOpenResult;
import com.hfstudio.guidenh.mixins.late.compat.thaumcraft.AccessorGuiResearchBrowser;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;

public class ThaumcraftResearchNavigation {

    @Nullable
    private static GuiScreen returnScreen;

    protected ThaumcraftResearchNavigation() {}

    public static BindingOpenResult open(ResearchItem item, GuideUiHost origin) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || item.isVirtual())
            return BindingOpenResult.failure("guidenh.binding.unavailable");
        GuiScreen previous = origin instanceof GuiScreen screen ? screen : minecraft.currentScreen;
        AccessorGuiResearchBrowser.guidenh$setSelectedCategory(item.category);
        GuiResearchBrowser.lastX = item.displayColumn;
        GuiResearchBrowser.lastY = item.displayRow;
        double x = item.displayColumn * 24 - 82;
        double y = item.displayRow * 24 - 70;
        GuiScreen target = new GuiResearchBrowser(x, y);
        returnScreen = previous;
        minecraft.displayGuiScreen(target);
        if (minecraft.currentScreen != target) {
            returnScreen = null;
            return BindingOpenResult.failure("guidenh.binding.unavailable");
        }
        return BindingOpenResult.success();
    }

    public static boolean returnToGuide() {
        if (returnScreen == null) return false;
        GuiScreen screen = returnScreen;
        returnScreen = null;
        Minecraft.getMinecraft()
            .displayGuiScreen(screen);
        return true;
    }

    public static void clear() {
        GuiScreen retained = returnScreen;
        returnScreen = null;
        if (retained instanceof GuideScreen guide) guide.onGuiClosed();
    }

    public static void onGuiOpened(@Nullable GuiScreen screen) {
        if (returnScreen != null && screen != returnScreen
            && !(screen instanceof GuiResearchBrowser)
            && !(screen instanceof GuiResearchRecipe)) clear();
    }
}
