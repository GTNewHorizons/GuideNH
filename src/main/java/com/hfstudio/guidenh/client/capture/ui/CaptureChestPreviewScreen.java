package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.InventoryBasic;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.gui.StandaloneContainerScreen;

public class CaptureChestPreviewScreen extends GuiChest implements StandaloneContainerScreen {

    private final GuiScreen parent;

    public CaptureChestPreviewScreen(GuiScreen parent, int rows) {
        super(CapturePreviewInventories.player(), new InventoryBasic("container.chest", false, rows * 9));
        this.parent = parent;
    }

    @Override
    public void updateStandaloneScreen() {}

    @Override
    public void onGuiClosed() {}

    @Override
    public void handleMouseInput() {}

    @Override
    protected void mouseClicked(int x, int y, int button) {}

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {}

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }
}
