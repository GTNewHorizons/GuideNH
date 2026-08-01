package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

import lombok.Getter;

@Getter
public class LytWidthBox extends LytVBox {

    private int preferredWidth;

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
        // preferredWidth <= 0 means "no fixed width constraint": signal full
        // width through the fullWidth mechanism (same pattern as
        // LytDetailsBlock/LytCodeBlock) so LayoutStyleExtractor serializes
        // size_w=100% and the authoritative Rust layout stretches this box to
        // the available content width. (Rust has no knowledge of preferredWidth
        // itself — serializing 0 as auto would shrink-wrap to content width.)
        setFullWidth(this.preferredWidth <= 0);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int constrainedWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        return super.computeBoxLayout(context, x, y, Math.max(1, constrainedWidth));
    }
}
