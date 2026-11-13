package com.hfstudio.guidenh.mixins.late.offscreen;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.integration.offscreen.GuideOffscreenHost;
import com.sbancuz.offscreen.api.HostUI;
import com.sbancuz.offscreen.window.Window;
import com.sbancuz.offscreen.window.input.FrameEvent;

@Mixin(value = Window.class, remap = false)
public abstract class MixinOffscreenWindow {

    @Shadow
    @Final
    private FrameEvent frameEvent;

    @Inject(
        method = "renderFrame",
        at = @At(value = "INVOKE", target = "Lcom/sbancuz/offscreen/window/Window;destroy()V"),
        cancellable = true)
    private void guidenh$requestGuideClose(CallbackInfo callback) {
        HostUI ui = ((Window) (Object) this).getCurrentUI();
        if (ui instanceof GuideOffscreenHost host) {
            frameEvent.closeRequested = false;
            host.requestClose();
            callback.cancel();
        }
    }
}
