package com.hfstudio.guidenh.mixins.early.lwjgl3ify;

import java.lang.ref.WeakReference;

import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.eigenraven.lwjgl3ify.client.TextFieldHandler;

@Mixin(value = TextFieldHandler.class, remap = false)
public interface AccessorTextFieldHandler {

    @Accessor("textInputDepth")
    static int guidenh$getTextInputDepth() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("textInputDepth")
    static void guidenh$setTextInputDepth(int depth) {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("focusedTextInput")
    static WeakReference<GuiTextField> guidenh$getFocusedTextInput() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("focusedTextInput")
    static void guidenh$setFocusedTextInput(WeakReference<GuiTextField> field) {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
