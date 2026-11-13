package com.hfstudio.guidenh.integration.offscreen;

import java.lang.ref.WeakReference;

import net.minecraft.client.gui.GuiTextField;

import com.hfstudio.guidenh.mixins.early.lwjgl3ify.AccessorTextFieldHandler;

import me.eigenraven.lwjgl3ify.api.InputEvents;
import me.eigenraven.lwjgl3ify.client.TextFieldHandler;

public class GuideOffscreenTextInput {

    private static final WeakReference<GuiTextField> NO_FIELD = new WeakReference<>(null);

    private int depth;
    private WeakReference<GuiTextField> focusedField = NO_FIELD;
    private String pendingText = "";
    private int savedDepth;
    private WeakReference<GuiTextField> savedField;
    private String savedText;
    private int enteredDepth;
    private WeakReference<GuiTextField> enteredField;

    public GuideOffscreenTextInput(boolean captureCurrent) {
        if (captureCurrent) {
            capture();
        }
    }

    public void enter() {
        savedDepth = AccessorTextFieldHandler.guidenh$getTextInputDepth();
        savedField = AccessorTextFieldHandler.guidenh$getFocusedTextInput();
        savedText = TextFieldHandler.textBuffer.toString();
        enteredDepth = depth;
        enteredField = focusedField;
        install(depth, focusedField, pendingText);
    }

    public void restore() {
        capture();
        try {
            if (depth != enteredDepth || focusedField != enteredField) {
                restoreNativeInput(savedDepth);
            }
            install(savedDepth, savedField, savedText);
        } finally {
            savedField = null;
            savedText = null;
            enteredField = null;
        }
    }

    public static void commit(String text) {
        InputEvents.injectTextEvent(new InputEvents.TextEvent(text));
    }

    public static void clearPendingText() {
        TextFieldHandler.textBuffer.setLength(0);
    }

    private void capture() {
        depth = AccessorTextFieldHandler.guidenh$getTextInputDepth();
        focusedField = AccessorTextFieldHandler.guidenh$getFocusedTextInput();
        pendingText = TextFieldHandler.textBuffer.toString();
    }

    private static void install(int depth, WeakReference<GuiTextField> field, String text) {
        AccessorTextFieldHandler.guidenh$setTextInputDepth(depth);
        AccessorTextFieldHandler.guidenh$setFocusedTextInput(field);
        StringBuilder buffer = TextFieldHandler.textBuffer;
        buffer.setLength(0);
        buffer.append(text);
    }

    private static void restoreNativeInput(int depth) {
        // Focus callbacks target the main SDL window even while a guide child screen is active.
        AccessorTextFieldHandler.guidenh$setTextInputDepth(depth > 0 ? 0 : 1);
        if (depth > 0) {
            InputEvents.beginTextInput();
        } else {
            InputEvents.endTextInput();
        }
    }
}
