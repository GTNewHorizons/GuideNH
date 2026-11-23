package com.hfstudio.guidenh.client.capture;

import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.client.capture.render.FinalFrameCaptureSourceProvider;
import com.hfstudio.guidenh.client.capture.render.GlCapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.render.GuiLayerCaptureSourceProvider;
import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.angelica.capture.AngelicaCapturePixelReaderProvider;
import com.hfstudio.guidenh.integration.angelica.capture.AngelicaCaptureRenderProbe;
import com.hfstudio.guidenh.integration.angelica.capture.AngelicaCaptureSourceProvider;
import com.hfstudio.guidenh.integration.angelica.capture.AngelicaOpenGlPixelReaderProvider;
import com.hfstudio.guidenh.integration.betterquesting.capture.BqCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.betterquesting.capture.BqPageBatchProvider;
import com.hfstudio.guidenh.integration.capture.GuideCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.capture.GuidePageBatchProvider;
import com.hfstudio.guidenh.integration.capture.ServerGuiBatchProvider;
import com.hfstudio.guidenh.integration.lwjgl3ify.capture.LwjglCaptureInputBridge;
import com.hfstudio.guidenh.integration.mui.capture.MuiCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.mui.capture.MuiPageBatchProvider;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2CaptureGeometryProvider;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2PageBatchProvider;
import com.hfstudio.guidenh.integration.nei.capture.NeiCaptureGeometryProvider;
import com.hfstudio.guidenh.integration.nei.capture.NeiPageBatchProvider;
import com.hfstudio.guidenh.integration.thaumcraft.capture.TcPageBatchProvider;

import cpw.mods.fml.common.FMLCommonHandler;

public class GuiCaptureBootstrap {

    private static boolean initialized;

    private GuiCaptureBootstrap() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        OpenGuiCaptureHotkey.initialize();
        CaptureRuntime runtime = CaptureRuntime.instance();
        runtime.registry()
            .registerReader(new GlCapturePixelReaderProvider());
        runtime.registry()
            .registerSource(new GuiLayerCaptureSourceProvider());
        runtime.registry()
            .registerSource(new FinalFrameCaptureSourceProvider());
        runtime.initialize();
        if (Mods.Angelica.isModLoaded()) {
            runtime.renderProbe(new AngelicaCaptureRenderProbe(), true);
            runtime.registry()
                .registerReader(new AngelicaOpenGlPixelReaderProvider());
            runtime.registry()
                .registerReader(new AngelicaCapturePixelReaderProvider());
            runtime.registry()
                .registerSource(new AngelicaCaptureSourceProvider(new GuiLayerCaptureSourceProvider()));
            runtime.registry()
                .registerSource(new AngelicaCaptureSourceProvider(new FinalFrameCaptureSourceProvider()));
        }
        CaptureClientConnection.instance()
            .onCatalogChanged(runtime::refreshBatchCatalog);
        runtime.registry()
            .registerBatch(new ServerGuiBatchProvider(CaptureClientConnection.instance()));
        runtime.registry()
            .registerGeometry(new GuideCaptureGeometryProvider());
        runtime.registry()
            .registerBatch(new GuidePageBatchProvider());
        if (Mods.ModularUI.isModLoaded()) runtime.registry()
            .registerGeometry(new MuiCaptureGeometryProvider());
        if (Mods.ModularUI.isModLoaded()) runtime.registry()
            .registerBatch(new MuiPageBatchProvider());
        if (Mods.ModularUI2.isModLoaded()) runtime.registry()
            .registerGeometry(new Mui2CaptureGeometryProvider());
        if (Mods.ModularUI2.isModLoaded()) runtime.registry()
            .registerBatch(new Mui2PageBatchProvider());
        if (Mods.NotEnoughItems.isModLoaded()) runtime.registry()
            .registerGeometry(new NeiCaptureGeometryProvider());
        if (Mods.Lwjgl3ify.isModLoaded()) {
            runtime.useNativeKeyboard();
            runtime.registry()
                .registerLifecycle(new LwjglCaptureInputBridge());
        }
        if (Mods.NotEnoughItems.isModLoaded()) runtime.registry()
            .registerBatch(new NeiPageBatchProvider());
        if (Mods.BetterQuesting.isModLoaded()) {
            runtime.registry()
                .registerGeometry(new BqCaptureGeometryProvider());
            runtime.registry()
                .registerBatch(new BqPageBatchProvider());
        }
        if (Mods.Thaumcraft.isModLoaded()) runtime.registry().registerBatch(new TcPageBatchProvider());
        MinecraftForge.EVENT_BUS.register(runtime);
        FMLCommonHandler.instance()
            .bus()
            .register(runtime);
    }
}
