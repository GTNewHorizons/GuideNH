package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.tileentity.TileEntityFurnace;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.client.gui.StandaloneContainerScreen;

public class CaptureFurnacePreviewScreen extends GuiFurnace implements StandaloneContainerScreen {

    private final GuiScreen parent;

    public CaptureFurnacePreviewScreen(GuiScreen parent, boolean burning) {
        super(CapturePreviewInventories.player(), snapshot(burning));
        this.parent = parent;
    }

    private static TileEntityFurnace snapshot(boolean burning) {
        TileEntityFurnace furnace = new TileEntityFurnace();
        if (burning) {
            furnace.furnaceBurnTime = 1600;
            furnace.currentItemBurnTime = 1600;
            furnace.furnaceCookTime = 100;
        }
        return furnace;
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
