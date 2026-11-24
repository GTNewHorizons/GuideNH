package com.hfstudio.guidenh.integration.capture;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.resources.I18n;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorGuiCaptureScreen;

public class VanillaCaptureGuiRenderer implements CaptureGuiRenderer {

    @Override
    public String id() {
        return "vanilla-menu";
    }

    @Override
    public boolean supports(CaptureContext context) {
        if (context.screen()
            .getClass() != GuiIngameMenu.class) return false;
        return ((AccessorGuiCaptureScreen) context.screen()).guidenh$getCaptureButtons()
            .stream()
            .allMatch(button -> button.getClass() == GuiButton.class || button.getClass() == GuiOptionButton.class);
    }

    @Override
    public CaptureRenderSnapshot snapshot(CaptureContext context, int mouseX, int mouseY, boolean includeBackdrop) {
        String title = I18n.format("menu.game");
        List<GuiButton> buttons = ((AccessorGuiCaptureScreen) context.screen()).guidenh$getCaptureButtons()
            .stream()
            .map(VanillaCaptureGuiRenderer::copyButton)
            .toList();
        int width = context.viewport()
            .logicalWidth();
        return () -> {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (includeBackdrop) context.screen()
                .drawDefaultBackground();
            new Gui().drawCenteredString(minecraft.fontRenderer, title, width / 2, 40, 0xFFFFFF);
            for (GuiButton button : buttons) button.drawButton(minecraft, mouseX, mouseY);
        };
    }

    private static GuiButton copyButton(GuiButton source) {
        GuiButton button = new GuiButton(
            source.id,
            source.xPosition,
            source.yPosition,
            source.width,
            source.height,
            source.displayString);
        button.enabled = source.enabled;
        button.visible = source.visible;
        return button;
    }
}
