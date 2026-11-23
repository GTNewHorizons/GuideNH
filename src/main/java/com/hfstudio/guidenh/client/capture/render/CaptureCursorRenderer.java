package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.ui.CaptureIcons;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public class CaptureCursorRenderer {

    private CaptureCursorRenderer() {}

    public static void draw(int x, int y) {
        if (x < 0 || y < 0) return;
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_FOG);
            GuideIconButton.drawIcon(Minecraft.getMinecraft(), CaptureIcons.CURSOR, x, y, 16, 16, 0xFFFFFFFF);
        }
    }
}
