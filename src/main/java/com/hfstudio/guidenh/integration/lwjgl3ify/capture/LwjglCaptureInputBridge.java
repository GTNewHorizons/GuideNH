package com.hfstudio.guidenh.integration.lwjgl3ify.capture;

import java.lang.ref.WeakReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import com.hfstudio.guidenh.client.capture.CaptureRuntime;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureInputEvent;
import com.hfstudio.guidenh.client.capture.api.CaptureLifecycleListener;
import com.hfstudio.guidenh.client.hotkey.OpenGuiCaptureHotkey;
import com.hfstudio.guidenh.guide.internal.input.GuideKeyBindingSupport;
import com.hfstudio.guidenh.mixins.early.compat.lwjgl3ify.AccessorCaptureTextInput;

import me.eigenraven.lwjgl3ify.api.InputEvents;

public class LwjglCaptureInputBridge implements CaptureLifecycleListener {

    private WeakReference<GuiTextField> previousInput;

    public static boolean key(InputEvents.KeyEvent event) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        int code = event.lwjgl2KeyCode == 0 ? event.lwjgl2ScanCode : event.lwjgl2KeyCode;
        if (!runtime.inputCaptured()) {
            if (!runtime.guiAvailable() || event.action != InputEvents.KeyAction.PRESSED
                || !GuideKeyBindingSupport.matchesKeyCode(OpenGuiCaptureHotkey.getHotkey(), code)) return false;
            runtime.requestOpen();
            return true;
        }
        int modifiers = event.sdlKeyModifiers;
        runtime.queueInput(
            new CaptureInputEvent(
                code,
                event.action != InputEvents.KeyAction.RELEASED,
                (modifiers & (Minecraft.isRunningOnMac ? 0xC00 : 0xC0)) != 0,
                (modifiers & 3) != 0,
                null));
        return true;
    }

    public static boolean text(InputEvents.TextEvent event) {
        CaptureRuntime runtime = CaptureRuntime.instance();
        if (!runtime.inputCaptured()) return false;
        runtime.queueInput(new CaptureInputEvent(0, false, false, false, event.text));
        return true;
    }

    @Override
    public void opened(CaptureContext context) {
        previousInput = AccessorCaptureTextInput.guidenh$getFocusedInput();
    }

    @Override
    public void closed(CaptureContext context) {
        if (CaptureRuntime.instance()
            .sourceMatches(Minecraft.getMinecraft().currentScreen))
            AccessorCaptureTextInput.guidenh$setFocusedInput(previousInput);
        previousInput = null;
    }
}
