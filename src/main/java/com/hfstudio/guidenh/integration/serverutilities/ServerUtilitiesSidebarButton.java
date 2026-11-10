package com.hfstudio.guidenh.integration.serverutilities;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuideScreen;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.events.client.CustomClickEvent;

@SideOnly(Side.CLIENT)
public class ServerUtilitiesSidebarButton {

    private static final ResourceLocation BUTTON_ID = new ResourceLocation(GuideNH.MODID, "open_guide");

    private ServerUtilitiesSidebarButton() {}

    public static void registerClickEvent() {
        MinecraftForge.EVENT_BUS.register(new ServerUtilitiesSidebarButton());
    }

    @SubscribeEvent
    public void onCustomClick(CustomClickEvent event) {
        if (!BUTTON_ID.equals(event.getID())) return;

        event.setCanceled(true);

        GuideScreen.openFromHomeHotkey();
    }

}
