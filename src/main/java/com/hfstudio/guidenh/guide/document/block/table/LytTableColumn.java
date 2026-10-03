package com.hfstudio.guidenh.guide.document.block.table;

import lombok.Getter;

public class LytTableColumn {

    @Getter
    int x;
    @Getter
    int width;
    @Getter
    int preferredWidth;

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }
}
