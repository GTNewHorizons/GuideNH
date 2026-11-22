package com.hfstudio.guidenh.client.capture;

import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.lwjgl3ify.capture.LwjglCaptureInputBridge;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2CaptureGeometryProvider;

import cpw.mods.fml.common.FMLCommonHandler;

public class GuiCaptureBootstrap {

    private static boolean initialized;

    private GuiCaptureBootstrap() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        OpenGuiCaptureHotkey.initialize();
        CaptureRuntime runtime = CaptureRuntime.instance();
        runtime.initialize();
        if (Mods.ModularUI2.isModLoaded()) runtime.registry()
            .registerGeometry(new Mui2CaptureGeometryProvider());
        if (Mods.Lwjgl3ify.isModLoaded()) {
            runtime.useNativeKeyboard();
            runtime.registry()
                .registerLifecycle(new LwjglCaptureInputBridge());
        }
        MinecraftForge.EVENT_BUS.register(runtime);
        FMLCommonHandler.instance()
            .bus()
            .register(runtime);
    }
}
