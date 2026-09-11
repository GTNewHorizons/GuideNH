package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import com.hfstudio.guidenh.guide.syntax.SyntaxSlotMatch;

/**
 * The slot a {@code SyntaxSlot} of another mod owns. The match carries the range, the values and the
 * writer, so the commit step only asks it what to write instead of knowing any slot of its own.
 */
public class SlotContext implements AutocompleteContext {

    private final SyntaxSlotMatch match;

    public SlotContext(SyntaxSlotMatch match) {
        this.match = match;
    }

    public SyntaxSlotMatch match() {
        return match;
    }

    @Override
    public int replaceStart() {
        return match.replaceStart();
    }

    @Override
    public int replaceEnd() {
        return match.replaceEnd();
    }

    @Override
    public String getPartialText() {
        return match.typedText();
    }

    @Override
    public boolean expandsTypedText() {
        return true;
    }
}
