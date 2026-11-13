package com.hfstudio.guidenh.guide.internal.input;

import java.util.BitSet;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuideInputContext {

    private static GuideInputContext active;

    private GuideInputContext previous;
    private int mouseX = -1;
    private int mouseY = -1;
    private long buttons;
    private final BitSet keys = new BitSet();
    private boolean focused;

    public void enter() {
        previous = active;
        active = this;
    }

    public void restore() {
        active = previous;
        previous = null;
    }

    public void setPointer(int x, int y) {
        mouseX = x;
        mouseY = y;
    }

    public void setButton(int button, boolean pressed) {
        if (button < 0 || button >= Long.SIZE) {
            return;
        }
        long mask = 1L << button;
        buttons = pressed ? buttons | mask : buttons & ~mask;
    }

    public void clearButtons() {
        buttons = 0L;
    }

    public void setKey(int keyCode, boolean pressed) {
        if (keyCode >= 0) {
            keys.set(keyCode, pressed);
        }
    }

    public void clearKeys() {
        keys.clear();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public static boolean isFocused() {
        return active == null || active.focused;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static int mouseX(int scaledWidth) {
        if (active != null) {
            return active.mouseX;
        }
        return Mouse.getX() * scaledWidth / Math.max(1, Minecraft.getMinecraft().displayWidth);
    }

    public static int mouseY(int scaledHeight) {
        if (active != null) {
            return active.mouseY;
        }
        return scaledHeight - Mouse.getY() * scaledHeight / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
    }

    public static boolean isButtonDown(int button) {
        if (active != null) {
            return button >= 0 && button < Long.SIZE && (active.buttons & (1L << button)) != 0L;
        }
        return Mouse.isButtonDown(button);
    }

    public static boolean isKeyDown(int keyCode) {
        return active != null ? active.focused && keyCode >= 0 && active.keys.get(keyCode)
            : Keyboard.isKeyDown(keyCode);
    }

    public static boolean isShiftKeyDown() {
        return isKeyDown(Keyboard.KEY_LSHIFT) || isKeyDown(Keyboard.KEY_RSHIFT);
    }
}
