package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;

@Mixin(EntityPlayerMP.class)
public class MixinCaptureServerContainer {

    @Inject(method = "closeContainer", at = @At("RETURN"))
    private void guidenh$releaseFactory(CallbackInfo callback) {
        CaptureServerCoordinator.instance()
            .forgetOrigin((EntityPlayerMP) (Object) this);
    }
}
