package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;

public class LytBindingCard extends LytQuoteBox {

    private final BindingSpec spec;

    public LytBindingCard(BindingSpec spec) {
        this.spec = spec;
    }

    public BindingSpec getBindingSpec() {
        return spec;
    }
}
