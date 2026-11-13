package com.hfstudio.guidenh.integration.offscreen;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.integration.Mods;

public class GuideOffscreenBridge {

    private GuideOffscreenBridge() {}

    public static boolean isAvailable() {
        return Mods.Offscreen.isModLoaded() && Minecraft.getMinecraft().theWorld != null;
    }

    @Nullable
    public static GuideScreen current() {
        return Mods.Offscreen.isModLoaded() ? GuideOffscreenController.current() : null;
    }

    public static boolean isHosted(GuideScreen screen) {
        return current() == screen;
    }

    public static boolean open(GuideScreen screen) {
        return isAvailable() && GuideOffscreenController.open(screen);
    }

    public static void toggle(GuideScreen screen) {
        if (isAvailable() || isHosted(screen)) {
            GuideOffscreenController.toggle(screen);
        }
    }

    public static boolean defer(GuideScreen screen, Runnable action) {
        return Mods.Offscreen.isModLoaded() && GuideOffscreenController.defer(screen, action);
    }

    public static boolean close(GuideScreen screen) {
        if (!isHosted(screen)) {
            return false;
        }
        GuideOffscreenController.close(screen);
        return true;
    }

    public static void shutdown() {
        if (Mods.Offscreen.isModLoaded()) {
            GuideOffscreenController.shutdown();
        }
    }
}
