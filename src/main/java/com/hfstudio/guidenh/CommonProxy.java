package com.hfstudio.guidenh;

import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;
import com.hfstudio.guidenh.integration.GuideNhIntegrationBootstrap;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhNetworkEvents;
import com.hfstudio.guidenh.network.capture.CaptureNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GuideNhNetwork.initCommon();
        CaptureNetwork.initCommon();
        CaptureServerCoordinator.instance()
            .initialize();
        GuideNhIntegrationBootstrap.preInitCommon();
        FMLCommonHandler.instance()
            .bus()
            .register(new GuideNhNetworkEvents());
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void completeInit(FMLLoadCompleteEvent event) {}
}
