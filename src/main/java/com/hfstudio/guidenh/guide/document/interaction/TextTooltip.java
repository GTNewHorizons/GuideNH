package com.hfstudio.guidenh.guide.document.interaction;

import lombok.Getter;

@Getter
public class TextTooltip implements GuideTooltip {

    private final String text;

    public TextTooltip(String text) {
        this.text = text;
    }

}
