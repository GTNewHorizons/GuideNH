package com.hfstudio.guidenh.guide.internal.markdown;

import net.minecraft.client.gui.GuiScreen;

public class CodeBlockClipboardService {

    public void copy(String text) {
        GuiScreen.setClipboardString(text);
    }
}
