package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LytTaskListItem extends LytListItem {

    private boolean checked;

    public LytTaskListItem() {
        // Extra 4px beyond LytListItem's LEVEL_MARGIN, matching the legacy
        // computeBoxLayout's x+4 offset for task item content.
        setPaddingLeft(LEVEL_MARGIN + 4);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int margin = LEVEL_MARGIN + 4;
        LytRect bounds = super.computeBoxLayout(context, x + 4, y, Math.max(1, availableWidth - margin));
        return bounds.expand(4, 0, 0, 0);
    }

    @Override
    protected boolean hasOwnMarker() {
        // The checkbox replaces the shared bullet / ordered number — the
        // superclass must not also paint a marker in the gutter.
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);
        LytRect bounds = getBounds();
        int boxSize = 7;
        int boxX = bounds.x() + 1;
        // Vertically center the checkbox on the marker's first-line text run
        // (bounds written back by Rust): the checkbox's vertical center lands
        // on the first line's vertical center, matching the bullet alignment.
        LytRect markerLine = getMarkerLineBounds();
        int boxY = markerLine.y() + (markerLine.height() - boxSize) / 2;
        int argb = SymbolicColor.BODY_TEXT.resolve(LightDarkMode.current());
        c.emit(new GuideRenderPrimitive.DrawBorder(boxX, boxY, boxSize, boxSize, 1, 1, 1, 1, argb));
        if (checked) {
            int fillArgb = SymbolicColor.LINK.resolve(LightDarkMode.current());
            c.emit(new GuideRenderPrimitive.FillRect(boxX + 2, boxY + 2, 3, 3, fillArgb));
        }
    }

    @Override
    public void render(RenderContext context) {
        LytRect bounds = getBounds();
        int boxSize = 7;
        int boxX = bounds.x() + 1;
        // Same marker-line anchor as computePrimitives: center the checkbox on
        // the first-line text run bounds (Rust layout authority).
        LytRect markerLine = getMarkerLineBounds(context);
        int boxY = markerLine.y() + (markerLine.height() - boxSize) / 2;
        context.drawBorder(new LytRect(boxX, boxY, boxSize, boxSize), context.resolveColor(SymbolicColor.BODY_TEXT), 1);
        if (checked) {
            context.fillRect(boxX + 2, boxY + 2, 3, 3, SymbolicColor.LINK);
        }
        super.render(context);
    }
}
