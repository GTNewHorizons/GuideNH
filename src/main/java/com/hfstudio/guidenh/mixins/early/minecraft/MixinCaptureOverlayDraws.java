package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;

@Mixin(Tessellator.class)
public class MixinCaptureOverlayDraws {

    @Shadow
    private int[] rawBuffer;
    @Shadow
    private int rawBufferIndex;
    @Shadow
    private int vertexCount;
    @Shadow
    private int drawMode;
    @Shadow
    private boolean hasTexture;
    @Shadow
    private boolean hasColor;
    @Shadow
    private boolean hasBrightness;
    @Shadow
    private boolean hasNormals;

    @Inject(method = "draw", at = @At("HEAD"))
    private void guidenh$recordOverlayDraw(CallbackInfoReturnable<Integer> callback) {
        CaptureRuntime.instance()
            .recordOverlayDraw(
                rawBuffer,
                vertexCount,
                rawBufferIndex,
                drawMode,
                hasTexture,
                hasColor,
                hasBrightness,
                hasNormals);
    }
}
