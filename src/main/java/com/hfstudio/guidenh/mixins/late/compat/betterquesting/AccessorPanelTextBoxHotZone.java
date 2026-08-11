package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import betterquesting.api2.client.gui.misc.IGuiRect;

@Mixin(targets = "betterquesting.api2.client.gui.panels.content.PanelTextBox$HotZone", remap = false)
public interface AccessorPanelTextBoxHotZone {

    @Accessor("location")
    IGuiRect guidenh$getLocation();

    @Accessor("link")
    Object guidenh$getLink();
}
