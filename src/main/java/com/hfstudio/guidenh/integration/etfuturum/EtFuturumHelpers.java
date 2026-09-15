package com.hfstudio.guidenh.integration.etfuturum;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;
import ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra;
import ganymedes01.etfuturum.client.skins.PlayerModelManager;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;

public class EtFuturumHelpers {

    private EtFuturumHelpers() {}

    @Nullable
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        return player != null && Mods.EtFuturum.isModLoaded() ? resolveSlimImpl(player) : null;
    }

    public static boolean isElytraStack(@Nullable ItemStack stack) {
        return stack != null && Mods.EtFuturum.isModLoaded() && isElytraStackImpl(stack);
    }

    public static boolean tryRenderElytraLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        return entity != null && Mods.EtFuturum.isModLoaded()
            && tryRenderElytraLayerImpl(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
    }

    @Nullable
    @Optional.Method(modid = "etfuturum")
    private static Boolean resolveSlimImpl(AbstractClientPlayer player) {
        return PlayerModelManager.isPlayerModelAlex(player);
    }

    @Optional.Method(modid = "etfuturum")
    private static boolean isElytraStackImpl(ItemStack stack) {
        return stack.getItem() instanceof ItemArmorElytra;
    }

    @Optional.Method(modid = "etfuturum")
    private static boolean tryRenderElytraLayerImpl(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        LayerBetterElytra.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
        return true;
    }
}
