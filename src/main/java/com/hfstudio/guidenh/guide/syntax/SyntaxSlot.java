package com.hfstudio.guidenh.guide.syntax;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;

/**
 * A part of the guide editor that this mod's own syntax does not describe, contributed by another mod.
 *
 * <p>
 * Everything the built-in editor completes - tags, attributes, values, markdown, frontmatter - is
 * declared through {@link SyntaxContributor} and needs no slot. A slot is for syntax of a contributor's
 * own: a directive inside a block, a mini language in a code fence, a field of a tag the contributor
 * compiles itself. The slot decides where it owns text, which values the editor offers there and how an
 * accepted value is written.
 *
 * <p>
 * Slots are asked before the editor's own resolvers, so a contributor's syntax wins over the generic
 * word completion inside the text it claims. The first slot that matches owns the caret, even when it
 * offers no values, so a contributor never sees this mod's guesses inside its own syntax.
 *
 * <p>
 * Register through {@code GuideBuilder.extension(SyntaxSlot.EXTENSION_POINT, slot)} for one guide, or
 * {@code GuideNhIntegrationRegistry.registerSyntaxSlot(slot)} to apply to every guide.
 */
public interface SyntaxSlot extends Extension {

    ExtensionPoint<SyntaxSlot> EXTENSION_POINT = new ExtensionPoint<>(SyntaxSlot.class);

    /** Short identifier used in diagnostics, for example the owning mod id. */
    String namespace();

    /**
     * Describes the slot under the caret.
     *
     * @param text        the whole document text
     * @param cursorIndex caret position in {@code text}
     * @param model       the syntax of the guide currently open, for looking up tags and attributes
     * @return the slot under the caret, or {@code null} when this slot owns nothing there
     */
    @Nullable
    SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model);

    /**
     * The range a double click inside this slot selects, or {@code null} to leave the selection to the
     * editor.
     */
    default @Nullable SyntaxSelection selection(String text, int cursorIndex) {
        return null;
    }
}
