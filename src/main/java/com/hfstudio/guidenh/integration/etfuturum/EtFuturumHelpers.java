package com.hfstudio.guidenh.integration.etfuturum;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.Optional;
import ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra;
import ganymedes01.etfuturum.client.skins.PlayerModelManager;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;

public class EtFuturumHelpers {

    private EtFuturumHelpers() {}

    @Nullable
    @Optional.Method(modid = "etfuturum")
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        return player == null ? null : PlayerModelManager.isPlayerModelAlex(player);
    }

    @Optional.Method(modid = "etfuturum")
    public static boolean isElytraStack(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemArmorElytra;
    }

    @Optional.Method(modid = "etfuturum")
    public static boolean tryRenderElytraLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        if (entity == null) {
            return false;
        }
        LayerBetterElytra.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
        return true;
    }
}
