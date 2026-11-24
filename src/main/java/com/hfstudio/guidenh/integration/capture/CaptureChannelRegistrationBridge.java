package com.hfstudio.guidenh.integration.capture;

import java.lang.reflect.Field;
import java.util.Set;

import cpw.mods.fml.common.network.FMLNetworkEvent;

public class CaptureChannelRegistrationBridge {

    private static final Field REGISTRATIONS = registrations();

    private CaptureChannelRegistrationBridge() {}

    private static Field registrations() {
        try {
            return FMLNetworkEvent.CustomPacketRegistrationEvent.class.getField("registrations");
        } catch (NoSuchFieldException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static boolean contains(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event, String channel) {
        // Forge's Guava field descriptor must stay outside the shaded dependency boundary.
        try {
            return ((Set<?>) REGISTRATIONS.get(event)).contains(channel);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }
}
