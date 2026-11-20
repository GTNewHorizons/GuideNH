package com.hfstudio.guidenh.guide.internal.binding;

import com.hfstudio.guidenh.guide.document.block.LytParagraph;

public class BindingPlaceholder extends LytParagraph {

    private final BindingSpec spec;
    private final String kind;

    public BindingPlaceholder(BindingSpec spec, String kind) {
        this.spec = spec;
        this.kind = kind;
        setStyleClass("Binding");
        setStyle(LOADING_STYLE);
        appendText(
            spec.key()
                .id());
    }

    public BindingSpec getSpec() {
        return spec;
    }

    public String getKind() {
        return kind;
    }
}
