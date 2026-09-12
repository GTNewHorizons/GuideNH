package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.SyntaxSlotMatch;

/**
 * The slot a {@code SyntaxSlot} of another mod owns. The match carries the range, the values and the
 * writer, so the commit step only asks it what to write instead of knowing any slot of its own.
 */
public class SlotContext implements AutocompleteContext {

    private final GuideSyntaxModel.SlotMatch slotMatch;

    public SlotContext(GuideSyntaxModel.SlotMatch slotMatch) {
        this.slotMatch = slotMatch;
    }

    public SyntaxSlotMatch match() {
        return slotMatch.match();
    }

    /** Namespace of the slot that answered, used when reporting that its writer failed. */
    public String slotNamespace() {
        return slotMatch.slot()
            .namespace();
    }

    @Override
    public int replaceStart() {
        return match().replaceStart();
    }

    @Override
    public int replaceEnd() {
        return match().replaceEnd();
    }

    @Override
    public String getPartialText() {
        return match().typedText();
    }

    @Override
    public boolean expandsTypedText() {
        return true;
    }
}
