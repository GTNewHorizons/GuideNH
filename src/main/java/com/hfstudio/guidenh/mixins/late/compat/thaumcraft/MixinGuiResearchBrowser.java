package com.hfstudio.guidenh.mixins.late.compat.thaumcraft;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.integration.thaumcraft.ThaumcraftResearchNavigation;

import thaumcraft.client.gui.GuiResearchBrowser;

@Mixin(value = GuiResearchBrowser.class, remap = false)
public class MixinGuiResearchBrowser {

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void guidenh$returnToGuide(char character, int keyCode, CallbackInfo callback) {
        if ((keyCode == 1 || keyCode == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode())
            && ThaumcraftResearchNavigation.returnToGuide()) callback.cancel();
    }
}
