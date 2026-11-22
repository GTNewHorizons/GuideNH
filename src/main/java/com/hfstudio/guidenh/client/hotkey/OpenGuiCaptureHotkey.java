package com.hfstudio.guidenh.client.hotkey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.guide.internal.input.GuideKeyBindingSupport;

import cpw.mods.fml.client.registry.ClientRegistry;

public class OpenGuiCaptureHotkey {

    public static final KeyBinding OPEN_GUI_CAPTURE_KEY = new KeyBinding(
        "key.guidenh.open_gui_capture",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");
    private static boolean wasDown;

    private OpenGuiCaptureHotkey() {}

    public static void initialize() {
        ClientRegistry.registerKeyBinding(OPEN_GUI_CAPTURE_KEY);
    }

    public static void poll() {
        if (!Display.isActive()) {
            wasDown = false;
            return;
        }
        boolean down = GuideKeyBindingSupport.isDown(OPEN_GUI_CAPTURE_KEY);
        if (down && !wasDown && Minecraft.getMinecraft().currentScreen != null) CaptureRuntime.instance()
            .open();
        wasDown = down;
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUI_CAPTURE_KEY;
    }
}
