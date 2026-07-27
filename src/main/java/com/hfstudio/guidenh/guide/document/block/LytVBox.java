package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

/**
 * Lays out its children vertically.
 * <p>
 * The Java layout pre-pass has been removed — children are laid out by the
 * Rust layout engine. This method is retained for compatibility (LytBox's
 * final computeLayout calls it) but returns a minimal rect since real
 * bounds are applied via {@link #applyExternalLayout}.
 */
public class LytVBox extends LytAxisBox {

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        // NOTE: The document pipeline no longer calls this method — children
        // are laid out by the Rust layout engine which is the authoritative
        // source for bounds. This return value is only a fallback for the
        // legacy layout() call chain (LytBox.computeLayout(final) expands the
        // LytRect(x,y,0,0) with padding/border, producing a non-zero rect).
        return new LytRect(x, y, 0, 0);
    }
}
