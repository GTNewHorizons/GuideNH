package com.hfstudio.guidenh.mixins.late.compat.mui2;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.api.UIFactory;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2ServerReopen;

@Mixin(value = GuiManager.class, remap = false)
public class MixinCaptureMui2Factory {

    @Inject(method = "open", at = @At("RETURN"))
    private static <D extends GuiData> void guidenh$rememberFactory(UIFactory<D> factory, D data, EntityPlayerMP player,
        CallbackInfo callback) {
        if (data instanceof PosGuiData position && position.getTileEntity() != null
            && player.openContainer instanceof ModularContainer container
            && container.getGuiData() == data)
            CaptureServerCoordinator.instance()
                .recordOrigin(player, new Mui2ServerReopen<>(factory, data, position.getTileEntity()));
    }
}
