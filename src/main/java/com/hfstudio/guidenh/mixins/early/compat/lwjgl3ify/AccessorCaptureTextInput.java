package com.hfstudio.guidenh.mixins.early.compat.lwjgl3ify;

import java.lang.ref.WeakReference;

import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.eigenraven.lwjgl3ify.client.TextFieldHandler;

@Mixin(value = TextFieldHandler.class, remap = false)
public interface AccessorCaptureTextInput {

    @Accessor("focusedTextInput")
    static WeakReference<GuiTextField> guidenh$getFocusedInput() {
        throw new UnsupportedOperationException();
    }

    @Accessor("focusedTextInput")
    static void guidenh$setFocusedInput(WeakReference<GuiTextField> input) {
        throw new UnsupportedOperationException();
    }
}
