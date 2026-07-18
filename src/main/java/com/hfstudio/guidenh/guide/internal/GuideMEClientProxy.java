package com.hfstudio.guidenh.guide.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.client.GuideNhClientTaskScheduler;
import com.hfstudio.guidenh.guide.PageAnchor;

public class GuideMEClientProxy extends GuideMEServerProxy {

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation guideId, @Nullable PageAnchor anchor) {
        GuideScreen.open(guideId, anchor);
        return true;
    }

    @Override
    public boolean reloadResources() {
        var mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        return GuideMEClientReloadDispatcher.dispatch(
            GuideNhClientTaskScheduler.isOnClientThread(),
            GuideNhClientTaskScheduler::execute,
            mc::refreshResources);
    }
}
