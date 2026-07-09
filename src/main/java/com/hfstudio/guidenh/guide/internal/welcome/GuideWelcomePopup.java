package com.hfstudio.guidenh.guide.internal.welcome;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.hfstudio.guidenh.config.ModConfig;

public class GuideWelcomePopup {

    private GuideWelcomePopup() {}

    public static void showIfNeeded(GuiScreen parent) {
        if (!ModConfig.ui.welcomePopupEnabled) {
            return;
        }

        GuideWelcomeState state = GuideWelcomeState.current();
        if (state == null || state.isSeen()) {
            return;
        }

        GuideWelcomeContent.LoadedContent content = GuideWelcomeContent.load();
        if (content.source()
            .trim()
            .isEmpty()) {
            return;
        }

        Minecraft.getMinecraft()
            .displayGuiScreen(new GuideWelcomeScreen(parent, state, content));
    }
}
