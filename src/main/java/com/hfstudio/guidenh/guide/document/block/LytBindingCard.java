package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytBindingCard extends LytQuoteBox implements InteractiveElement {

    private final BindingSpec spec;
    @Nullable
    private final InteractiveElement action;

    public LytBindingCard(BindingSpec spec) {
        this(spec, null);
    }

    public LytBindingCard(BindingSpec spec, @Nullable InteractiveElement action) {
        this.spec = spec;
        this.action = action;
    }

    public BindingSpec getBindingSpec() {
        return spec;
    }

    @Override
    public boolean mouseClicked(GuideUiHost host, int x, int y, int button, boolean doubleClick) {
        return action != null && action.mouseClicked(host, x, y, button, doubleClick);
    }
}
