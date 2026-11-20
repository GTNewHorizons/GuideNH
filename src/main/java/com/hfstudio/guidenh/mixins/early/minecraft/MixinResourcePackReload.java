package com.hfstudio.guidenh.mixins.early.minecraft;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.FMLLog;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinResourcePackReload {

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void guidenh$closeResourcePackArchives(List<IResourcePack> packs, CallbackInfo ci) {
        for (IResourcePack pack : packs) {
            if (pack instanceof FileResourcePack archive) {
                try {
                    archive.close();
                } catch (IOException exception) {
                    FMLLog.getLogger()
                        .warn("Failed to close resource pack archive {}", pack.getPackName(), exception);
                }
            }
        }
    }
}
