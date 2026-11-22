package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import com.hfstudio.guidenh.guide.editor.SceneEditorIcon;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public class CaptureIconButton extends GuiButton {

    private final SceneEditorIcon icon;
    private final String tooltipKey;
    private final Runnable action;
    private boolean active;

    public CaptureIconButton(int id, SceneEditorIcon icon, String tooltipKey, Runnable action) {
        super(id, 0, 0, 24, 24, "");
        this.icon = icon;
        this.tooltipKey = tooltipKey;
        this.action = action;
    }

    public void setActive(boolean value) {
        active = value;
    }

    public String tooltip() {
        return I18n.format(tooltipKey);
    }

    public void activate() {
        if (enabled) action.run();
    }

    public boolean hovered() {
        return field_146123_n;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        if (active || field_146123_n) {
            drawRect(
                xPosition + 1,
                yPosition + 1,
                xPosition + width - 1,
                yPosition + height - 1,
                active ? 0xFF495862 : 0xFF353D46);
        }
        GuideIconButton
            .drawIcon(minecraft, icon, xPosition + 4, yPosition + 4, 16, 16, enabled ? 0xFFFFFFFF : 0x70FFFFFF);
    }
}
