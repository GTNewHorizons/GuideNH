package com.hfstudio.guidenh.mixins.late.compat;

import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.client.gui2.editors.GuiTextEditor;

@Mixin(value = GuiTextEditor.class, remap = false)
public abstract class MixinGuiTextEditor {

    @Inject(method = "initPanel", at = @At("TAIL"))
    private void guidenh$addGuideLinkMacro(CallbackInfo ci) {
        ((GuiTextEditor) (Object) this).addPanel(
            new PanelButtonStorage<>(
                new GuiTransform(GuiAlign.TOP_LEFT, 16, 16, 100, 16, 0),
                2,
                I18n.format("guidenh.compat.bq.insert_guide_link"),
                "[guide] [/guide]"));
    }
}
