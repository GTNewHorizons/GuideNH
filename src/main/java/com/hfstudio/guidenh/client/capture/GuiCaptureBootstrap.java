package com.hfstudio.guidenh.client.capture;

import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;

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
        FMLCommonHandler.instance()
            .bus()
            .register(runtime);
    }
}
