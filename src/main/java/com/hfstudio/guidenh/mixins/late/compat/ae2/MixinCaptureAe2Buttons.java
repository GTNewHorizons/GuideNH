package com.hfstudio.guidenh.mixins.late.compat.ae2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

import appeng.client.gui.widgets.GuiImgButton;

@Mixin(value = GuiImgButton.class, remap = false)
public class MixinCaptureAe2Buttons {

    @Inject(method = "drawButton", remap = true, at = @At("HEAD"), require = 1)
    private void guidenh$recordDrawnButton(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo callback) {
        CaptureRuntime.instance()
            .recordDrawnButton((GuiButton) (Object) this);
    }
}
