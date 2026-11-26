package com.hfstudio.guidenh.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideNhNetworkEvents {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GuideNhStructureRequestHandler.expireTransfers();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player instanceof EntityPlayerMP) {
            GuideNhStructureRuntime.getServerSessionStore()
                .reset(player.getUniqueID());
            GuideNhNetwork.channel()
                .sendTo(new GuideNhServerHelloMessage(), (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        if (player instanceof EntityPlayerMP) {
            GuideNhStructureRequestHandler.clearTransfers(player.getUniqueID());
            GuideNhServerRequests.instance()
                .clear(player.getUniqueID());
            GuideNhStructureRuntime.getServerSessionStore()
                .clear(player.getUniqueID());
        }
    }
}
