package com.hfstudio.guidenh.integration.offscreen;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.hfstudio.guidenh.guide.internal.input.GuideInputContext;
import com.hfstudio.guidenh.mixins.late.offscreen.AccessorOffscreenWindow;
import com.sbancuz.offscreen.scope.Scope;
import com.sbancuz.offscreen.window.Window;

public class GuideOffscreenScope implements Scope {

    private final Supplier<GuiScreen> screen;
    private final GuideInputContext input = new GuideInputContext();
    private final GuideOffscreenTextInput textInput;
    private Window window;
    private GuiScreen savedScreen;
    private int savedWidth;
    private int savedHeight;
    private boolean savedFocus;
    private boolean savedRepeat;
    private boolean savedGrab;
    private boolean savedSkipRenderWorld;
    private boolean entered;

    public GuideOffscreenScope(Supplier<GuiScreen> screen) {
        this.screen = screen;
        this.textInput = new GuideOffscreenTextInput(screen.get().width > 0);
    }

    public void bind(Window window) {
        this.window = window;
    }

    @Override
    public void enter() {
        if (entered) {
            throw new IllegalStateException("Guide window scope is already entered");
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        savedScreen = minecraft.currentScreen;
        savedWidth = minecraft.displayWidth;
        savedHeight = minecraft.displayHeight;
        savedFocus = minecraft.inGameHasFocus;
        savedRepeat = Keyboard.areRepeatEventsEnabled();
        savedGrab = Mouse.isGrabbed();
        savedSkipRenderWorld = minecraft.skipRenderWorld;
        input.setFocused(((AccessorOffscreenWindow) (Object) window).guidenh$isFocused());
        input.enter();
        entered = true;
        try {
            textInput.enter();
            minecraft.displayWidth = Math.max(1, window.getPixelWidth());
            minecraft.displayHeight = Math.max(1, window.getPixelHeight());
            show(screen.get());
        } catch (RuntimeException | Error error) {
            restore();
            throw error;
        }
    }

    public void show(GuiScreen activeScreen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.currentScreen = activeScreen;
        minecraft.inGameHasFocus = false;
        Keyboard.enableRepeatEvents(true);
    }

    public void restoreMainScreen() {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.currentScreen = savedScreen;
        minecraft.inGameHasFocus = savedFocus;
    }

    @Override
    public void restore() {
        if (!entered) {
            return;
        }
        try {
            textInput.restore();
        } finally {
            restoreMainScreen();
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.displayWidth = savedWidth;
            minecraft.displayHeight = savedHeight;
            minecraft.skipRenderWorld = savedSkipRenderWorld;
            Keyboard.enableRepeatEvents(savedRepeat);
            if (Mouse.isGrabbed() != savedGrab) {
                Mouse.setGrabbed(savedGrab);
            }
            input.restore();
            savedScreen = null;
            entered = false;
        }
    }

    public GuideInputContext input() {
        return input;
    }
}
