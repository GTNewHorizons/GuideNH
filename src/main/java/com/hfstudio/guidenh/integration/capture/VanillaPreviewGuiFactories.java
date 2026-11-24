package com.hfstudio.guidenh.integration.capture;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureClientGuiFactory;
import com.hfstudio.guidenh.client.capture.ui.CaptureChestPreviewScreen;
import com.hfstudio.guidenh.client.capture.ui.CaptureFurnacePreviewScreen;

public class VanillaPreviewGuiFactories {

    private VanillaPreviewGuiFactories() {}

    private static boolean available(CaptureBatchContext context) {
        return Minecraft.getMinecraft().thePlayer != null
            && Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null;
    }

    public static void register(ClientGuiBatchProvider provider) {
        provider.register(
            new CaptureClientGuiFactory(
                "preview_chest",
                "Chest preview",
                CaptureChestPreviewScreen.class,
                VanillaPreviewGuiFactories::available,
                "A player with an empty carried stack is required",
                parent -> new CaptureChestPreviewScreen(parent, 3)));
        provider.register(
            new CaptureClientGuiFactory(
                "preview_large_chest",
                "Double chest preview",
                CaptureChestPreviewScreen.class,
                VanillaPreviewGuiFactories::available,
                "A player with an empty carried stack is required",
                parent -> new CaptureChestPreviewScreen(parent, 6)));
        provider.register(
            new CaptureClientGuiFactory(
                "preview_furnace",
                "Furnace preview",
                CaptureFurnacePreviewScreen.class,
                VanillaPreviewGuiFactories::available,
                "A player with an empty carried stack is required",
                parent -> new CaptureFurnacePreviewScreen(parent, false)));
        provider.register(
            new CaptureClientGuiFactory(
                "preview_burning_furnace",
                "Burning furnace preview",
                CaptureFurnacePreviewScreen.class,
                VanillaPreviewGuiFactories::available,
                "A player with an empty carried stack is required",
                parent -> new CaptureFurnacePreviewScreen(parent, true)));
    }
}
