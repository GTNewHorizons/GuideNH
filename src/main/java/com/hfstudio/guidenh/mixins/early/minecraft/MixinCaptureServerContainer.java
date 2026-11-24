package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;
import com.hfstudio.guidenh.integration.capture.VanillaEntityGuiProvider.EntityAction;

@Mixin(EntityPlayerMP.class)
public class MixinCaptureServerContainer {

    @Inject(method = "displayGUIChest", at = @At("RETURN"))
    private void guidenh$rememberMinecartGui(IInventory inventory, CallbackInfo callback) {
        if (inventory instanceof EntityMinecartContainer minecart) CaptureServerCoordinator.instance()
            .recordObjectOrigin((EntityPlayerMP) (Object) this, new EntityAction(minecart));
    }

    @Inject(method = "displayGUIHorse", at = @At("RETURN"))
    private void guidenh$rememberHorseGui(EntityHorse horse, IInventory inventory, CallbackInfo callback) {
        CaptureServerCoordinator.instance()
            .recordObjectOrigin((EntityPlayerMP) (Object) this, new EntityAction(horse));
    }

    @Inject(method = "displayGUIMerchant", at = @At("RETURN"))
    private void guidenh$rememberMerchantGui(IMerchant merchant, String name, CallbackInfo callback) {
        if (merchant instanceof EntityVillager villager) CaptureServerCoordinator.instance()
            .recordObjectOrigin((EntityPlayerMP) (Object) this, new EntityAction(villager));
    }

    @Inject(method = "closeContainer", at = @At("RETURN"))
    private void guidenh$releaseFactory(CallbackInfo callback) {
        CaptureServerCoordinator.instance()
            .forgetOrigin((EntityPlayerMP) (Object) this);
    }
}
