package com.hfstudio.guidenh.mixins.late.compat.mui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.modularui.common.builder.UIInfo;
import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;
import com.hfstudio.guidenh.integration.mui.capture.MuiServerReopen;

@Mixin(value = UIInfo.class, remap = false)
public class MixinCaptureMuiFactory {

    @Unique
    private int guidenh$previousWindow;

    @Inject(
        method = "open(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;III)V",
        at = @At("HEAD"))
    private void guidenh$rememberWindow(EntityPlayer player, World world, int x, int y, int z, CallbackInfo callback) {
        if (player instanceof EntityPlayerMP serverPlayer) guidenh$previousWindow = serverPlayer.openContainer.windowId;
    }

    @Inject(
        method = "open(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;III)V",
        at = @At("RETURN"))
    private void guidenh$rememberFactory(EntityPlayer player, World world, int x, int y, int z, CallbackInfo callback) {
        if (!(player instanceof EntityPlayerMP serverPlayer)
            || serverPlayer.openContainer.windowId == guidenh$previousWindow
            || !world.blockExists(x, y, z)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null) CaptureServerCoordinator.instance()
            .recordOrigin(serverPlayer, new MuiServerReopen((UIInfo<?, ?>) (Object) this, tile));
    }
}
