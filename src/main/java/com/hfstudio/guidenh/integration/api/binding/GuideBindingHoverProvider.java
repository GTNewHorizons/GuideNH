package com.hfstudio.guidenh.integration.api.binding;

import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

public interface GuideBindingHoverProvider {

    @Nullable
    BindingHoverTarget findHoveredBinding(GuiScreen screen, int mouseX, int mouseY);
}
