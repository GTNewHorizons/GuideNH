package com.hfstudio.guidenh.integration.offscreen;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.client.GuideNhClientTaskScheduler;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.sbancuz.offscreen.api.OffscreenAPI;
import com.sbancuz.offscreen.window.Window;

public class GuideOffscreenController {

    private static GuideOffscreenHost host;
    private static Window window;
    private static boolean transferPending;

    private GuideOffscreenController() {}

    @Nullable
    public static GuideScreen current() {
        return host != null ? host.guide() : null;
    }

    public static boolean open(GuideScreen screen) {
        if (host != null || !GuideOffscreenBridge.isAvailable()) {
            return false;
        }
        Window created = null;
        GuideOffscreenHost candidate = null;
        try {
            candidate = new GuideOffscreenHost(screen);
            GuideOffscreenHost adapter = candidate;
            created = OffscreenAPI.open(() -> screen, ignored -> adapter);
            if (!OffscreenAPI.isOpen(created) || OffscreenAPI.getWindow(created.getId()) != created) {
                return false;
            }
            candidate.bind(created);
            created.setTitle(GuideNH.MODNAME);
            Minecraft minecraft = Minecraft.getMinecraft();
            int width = ModConfig.ui.guideWindowWidth > 0 ? ModConfig.ui.guideWindowWidth : minecraft.displayWidth;
            int height = ModConfig.ui.guideWindowHeight > 0 ? ModConfig.ui.guideWindowHeight : minecraft.displayHeight;
            OffscreenAPI.resize(created, Math.max(320, width), Math.max(240, height));
            host = candidate;
            window = created;
            return true;
        } catch (RuntimeException | LinkageError error) {
            GuideDebugLog.error("Unable to open the additional guide window", error);
            return false;
        } finally {
            if (created != null && created != window) {
                candidate.retainGuide();
                candidate.dispose();
                try {
                    OffscreenAPI.close(created);
                } catch (RuntimeException | LinkageError error) {
                    GuideDebugLog.error("Unable to clean up the additional guide window", error);
                }
            }
        }
    }

    public static void toggle(GuideScreen screen) {
        if (transferPending) {
            return;
        }
        transferPending = true;
        GuideNhClientTaskScheduler.enqueue(() -> {
            try {
                if (current() == screen) {
                    moveToMainWindow(screen);
                } else if (Minecraft.getMinecraft().currentScreen == screen) {
                    moveToAdditionalWindow(screen);
                }
            } finally {
                transferPending = false;
            }
        });
    }

    private static void moveToAdditionalWindow(GuideScreen screen) {
        if (!GuideOffscreenBridge.isAvailable()) {
            return;
        }
        screen.prepareWindowTransfer();
        try {
            if (!open(screen)) {
                return;
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.displayGuiScreen(null);
            if (minecraft.currentScreen == screen) {
                host.retainGuide();
                OffscreenAPI.close(window);
                return;
            }
            Keyboard.enableRepeatEvents(false);
            ModConfig.ui.guideInSeparateWindow = true;
            ModConfig.save();
        } finally {
            screen.finishWindowTransfer();
        }
    }

    private static void moveToMainWindow(GuideScreen screen) {
        GuideOffscreenHost previous = host;
        if (previous == null || previous.guide() != screen) {
            return;
        }
        rememberWindowSize();
        if (!screen.showInMainWindow()) {
            return;
        }
        previous.retainGuide();
        OffscreenAPI.close(window);
        ModConfig.ui.guideInSeparateWindow = false;
        ModConfig.save();
    }

    public static boolean defer(GuideScreen screen, Runnable action) {
        GuideOffscreenHost current = host;
        if (current == null || current.guide() != screen) {
            return false;
        }
        if (!current.isDispatching()) {
            current.enqueue(action);
            return true;
        }
        return false;
    }

    public static void close(GuideScreen screen) {
        GuideOffscreenHost closing = host;
        GuideNhClientTaskScheduler.enqueue(() -> {
            if (host != closing || closing == null || closing.guide() != screen) {
                return;
            }
            closing.closeGuide();
            OffscreenAPI.close(window);
        });
    }

    public static void shutdown() {
        if (host == null) {
            return;
        }
        host.closeGuide();
        OffscreenAPI.close(window);
    }

    public static void onDisposed(GuideOffscreenHost disposed, boolean transferred) {
        if (host != disposed) {
            return;
        }
        rememberWindowSize();
        host = null;
        window = null;
        if (!transferred) {
            ModConfig.save();
        }
    }

    private static void rememberWindowSize() {
        if (window != null) {
            ModConfig.ui.guideWindowWidth = Math.max(1, window.getPixelWidth());
            ModConfig.ui.guideWindowHeight = Math.max(1, window.getPixelHeight());
        }
    }
}
