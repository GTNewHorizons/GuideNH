package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureTextField extends GuiTextField {

    private CaptureRect frame;

    public CaptureTextField(FontRenderer font, int x, int y, int width, int height) {
        super(font, x, y, width, height);
        setEnableBackgroundDrawing(false);
        layout(x, y, width, height);
    }

    public void layout(int x, int y, int width, int height) {
        frame = new CaptureRect(x, y, width, height);
        xPosition = x + 4;
        yPosition = y + (height - 8) / 2;
        this.width = width - 8;
        this.height = 8;
    }

    public boolean contains(int x, int y) {
        return frame.contains(x, y);
    }

    @Override
    public void drawTextBox() {
        if (!getVisible()) return;
        Gui.drawRect(
            (int) frame.x() - 1,
            (int) frame.y() - 1,
            (int) frame.right() + 1,
            (int) frame.bottom() + 1,
            isFocused() ? CaptureUiColors.BORDER : CaptureUiColors.ROW);
        Gui.drawRect(
            (int) frame.x(),
            (int) frame.y(),
            (int) frame.right(),
            (int) frame.bottom(),
            CaptureUiColors.INPUT);
        super.drawTextBox();
    }

    @Override
    public void mouseClicked(int x, int y, int button) {
        if (frame.contains(x, y)) {
            x = Math.clamp(x, xPosition, xPosition + width - 1);
            y = Math.clamp(y, yPosition, yPosition + height - 1);
        }
        super.mouseClicked(x, y, button);
    }
}
