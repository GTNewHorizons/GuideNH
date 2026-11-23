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
    public static final KeyBinding TOGGLE_CAPTURE_INTERFACE_KEY = new KeyBinding(
        "key.guidenh.toggle_capture_interface",
        Keyboard.KEY_F6,
        "key.categories.guidenh");
    private static boolean interfaceWasDown;

    private OpenGuiCaptureHotkey() {}

    public static void initialize() {
        ClientRegistry.registerKeyBinding(OPEN_GUI_CAPTURE_KEY);
        ClientRegistry.registerKeyBinding(TOGGLE_CAPTURE_INTERFACE_KEY);
    }

    public static void poll() {
        if (!Display.isActive()) {
            wasDown = false;
            interfaceWasDown = false;
            return;
        }
        boolean down = GuideKeyBindingSupport.isDown(OPEN_GUI_CAPTURE_KEY);
        CaptureRuntime runtime = CaptureRuntime.instance();
        if (down && !wasDown && Minecraft.getMinecraft().currentScreen != null) {
            if (!runtime.active()) runtime.open();
            else if (!runtime.captureTextInputFocused()) runtime.toggleCaptureInterface();
        }
        wasDown = down;
        boolean interfaceDown = GuideKeyBindingSupport.isDown(TOGGLE_CAPTURE_INTERFACE_KEY);
        if (interfaceDown && !interfaceWasDown && runtime.active()) runtime.toggleCaptureInterface();
        interfaceWasDown = interfaceDown;
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUI_CAPTURE_KEY;
    }
}
