package com.hfstudio.guidenh.network.capture;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class CaptureNetwork {

    public static final String CHANNEL_NAME = "guidenh_capture";
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private CaptureNetwork() {}

    public static SimpleNetworkWrapper channel() {
        return CHANNEL;
    }

    public static void initCommon() {
        CHANNEL.registerMessage(CaptureRequestHandler.class, CaptureRequestMessage.class, 1, Side.SERVER);
    }

    public static void initClient() {
        CHANNEL.registerMessage(CaptureCapabilityHandler.class, CaptureCapabilityMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(CaptureReplyHandler.class, CaptureReplyMessage.class, 2, Side.CLIENT);
    }
}
