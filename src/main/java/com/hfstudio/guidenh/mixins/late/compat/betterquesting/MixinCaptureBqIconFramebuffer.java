package com.hfstudio.guidenh.mixins.late.compat.betterquesting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "betterquesting.api2.client.gui.panels.lists.CanvasQuestLine$QuestButtonCache", remap = false)
public class MixinCaptureBqIconFramebuffer {

    @Unique
    private int guidenh$captureDrawFramebuffer;

    @Unique
    private int guidenh$captureReadFramebuffer;

    @Inject(
        method = "getFramebuffer",
        at = @At(value = "NEW", target = "net/minecraft/client/shader/Framebuffer"),
        require = 0)
    private void guidenh$rememberIconFramebuffer(Minecraft minecraft, CallbackInfoReturnable<Framebuffer> callback) {
        if (!OpenGlHelper.framebufferSupported) return;
        guidenh$captureDrawFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        guidenh$captureReadFramebuffer = GLContext.getCapabilities().OpenGL30
            ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            : guidenh$captureDrawFramebuffer;
    }

    @Inject(
        method = "getFramebuffer",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopAttrib()V"),
        require = 0)
    private void guidenh$restoreIconFramebuffer(Minecraft minecraft, CallbackInfoReturnable<Framebuffer> callback) {
        if (!OpenGlHelper.framebufferSupported) return;
        // Color-buffer attributes must be restored against the framebuffer that supplied them.
        OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, guidenh$captureDrawFramebuffer);
        if (GLContext.getCapabilities().OpenGL30)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, guidenh$captureReadFramebuffer);
    }
}
