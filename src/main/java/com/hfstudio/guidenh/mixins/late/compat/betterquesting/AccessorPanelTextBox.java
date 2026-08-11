package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import betterquesting.api2.client.gui.panels.content.PanelTextBox;

@Mixin(value = PanelTextBox.class, remap = false)
public interface AccessorPanelTextBox {

    @Accessor("hotZones")
    List<Object> guidenh$getHotZones();
}
