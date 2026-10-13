package com.hfstudio.guidenh.guide.document.block.table;

import lombok.Getter;

public class LytTableColumn {

    int x;
    int width;
    @Getter
    int preferredWidth;
    @Getter
    int preferredWidthPercent;

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
        this.preferredWidthPercent = 0;
    }

    public void setPreferredWidthPercent(int preferredWidthPercent) {
        this.preferredWidthPercent = Math.max(0, Math.min(100, preferredWidthPercent));
        this.preferredWidth = 0;
    }
}
