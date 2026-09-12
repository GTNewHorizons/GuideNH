package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import com.hfstudio.guidenh.guide.syntax.GuideSyntaxModel;
import com.hfstudio.guidenh.guide.syntax.SyntaxSlotMatch;

/** The slot a {@code SyntaxSlot} of another mod owns. */
public class SlotContext implements AutocompleteContext {

    private final GuideSyntaxModel.SlotMatch slotMatch;

    public SlotContext(GuideSyntaxModel.SlotMatch slotMatch) {
        this.slotMatch = slotMatch;
    }

    public SyntaxSlotMatch match() {
        return slotMatch.match();
    }

    public String slotNamespace() {
        try {
            String namespace = slotMatch.slot()
                .namespace();
            if (namespace != null && !namespace.isEmpty()) {
                return namespace;
            }
        } catch (RuntimeException e) {}
        return slotMatch.slot()
            .getClass()
            .getSimpleName();
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
