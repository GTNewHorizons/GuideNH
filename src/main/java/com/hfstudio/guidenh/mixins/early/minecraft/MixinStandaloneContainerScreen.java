package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.gui.StandaloneContainerScreen;

@Mixin(GuiContainer.class)
public class MixinStandaloneContainerScreen {

    @Inject(method = "updateScreen", at = @At("HEAD"), cancellable = true)
    private void guidenh$updateStandaloneScreen(CallbackInfo callback) {
        if ((Object) this instanceof StandaloneContainerScreen screen) {
            screen.updateStandaloneScreen();
            callback.cancel();
        }
    }
}
