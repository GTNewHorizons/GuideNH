package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S30PacketWindowItems;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;

@Mixin(NetHandlerPlayClient.class)
public class MixinCaptureContainerSynchronization {

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    private void guidenh$captureWindowReady(S30PacketWindowItems packet, CallbackInfo callback) {
        CaptureClientConnection.instance()
            .windowSynchronized(packet.func_148911_c());
    }
}
