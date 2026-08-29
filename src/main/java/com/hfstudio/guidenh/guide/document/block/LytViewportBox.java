package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;

import lombok.Setter;

/**
 * A scroll viewport: a plain vertical container that clips its children to its
 * own bounds. Used by scroll containers (code blocks, size boxes, details
 * blocks) as the single scrollable region — the framework's children-clip
 * semantics then do the clipping, and the owner moves the content child's
 * bounds to scroll.
 * <p>
 * When the owner forces a viewport height, it is declared through
 * {@link #setExplicitHeight(int)} so the layout engine reserves exactly that
 * height; otherwise the viewport grows with its content (no scrolling).
 */
@Setter
public class LytViewportBox extends LytVBox {

    private int explicitHeight = -1;

    @Override
    public int getExplicitHeight() {
        return explicitHeight;
    }

    @Override
    public @Nullable LytRect getChildrenClipRect() {
        // Clip children to the viewport: the content may be taller and scrolls
        // beneath. When not scrollable the bounds equal the content bounds, so
        // the clip is a no-op.
        var b = getBounds();
        return b == null || b.isEmpty() ? null : b;
    }
}
