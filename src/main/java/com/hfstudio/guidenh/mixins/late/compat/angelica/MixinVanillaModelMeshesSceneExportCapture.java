package com.hfstudio.guidenh.mixins.late.compat.angelica;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelChest;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.rendering.tesr.VanillaModelMeshes;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

@Mixin(value = VanillaModelMeshes.class, remap = false)
public abstract class MixinVanillaModelMeshesSceneExportCapture {

    @Inject(method = "renderChest", at = @At("HEAD"), cancellable = true, require = 0)
    private static void guidenh$captureChest(ModelChest model, ResourceLocation texture, boolean large,
        CallbackInfo ci) {
        if (GuideSiteSceneTessellatorCapture.getActive() == null) {
            return;
        }
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        model.renderAll();
        ci.cancel();
    }
}
