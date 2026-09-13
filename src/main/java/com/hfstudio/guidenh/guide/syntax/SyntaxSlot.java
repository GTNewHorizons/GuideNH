package com.hfstudio.guidenh.guide.syntax;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * A part of the guide editor that the built-in syntax does not describe: a slot owns text of its own and decides.
 */
public interface SyntaxSlot extends Extension {

    ExtensionPoint<SyntaxSlot> EXTENSION_POINT = new ExtensionPoint<>(SyntaxSlot.class);

    String namespace();

    @Nullable
    SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model);

    /** The range a double click inside this slot selects, or {@code null} to leave the selection to the editor. */
    default @Nullable SyntaxSelection selection(String text, int cursorIndex) {
        return null;
    }
}
