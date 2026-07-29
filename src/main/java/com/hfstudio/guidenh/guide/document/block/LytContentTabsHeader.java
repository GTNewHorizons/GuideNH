package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

/**
 * The clickable tab strip of a {@link LytContentTabsBlock}: wraps tab titles
 * into rows and draws them (selected/idle style + active underline). A real
 * block, so the Rust layout treats the tabs block as three plain children
 * (title, header, active body) with no spacers or pins.
 * <p>
 * Measurement goes through {@link GuideText} (unified text pipeline), keeping
 * measure and render on the same font.
 */
public class LytContentTabsHeader extends LytBlock implements InteractiveElement {

    private static final int HEADER_GAP_X = 10;
    private static final int HEADER_GAP_Y = 5;
    private static final int HEADER_PAD_X = 2;
    private static final int HEADER_PAD_TOP = 1;
    private static final int HEADER_PAD_BOTTOM = 5;
    private static final int ACTIVE_RULE_THICKNESS = 2;

    private static final ResolvedTextStyle SELECTED_STYLE = new ResolvedTextStyle(
        1.0f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        "",
        new ConstantColor(0xFFF4F7FB),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false,
        0.0f);
    private static final ResolvedTextStyle IDLE_STYLE = new ResolvedTextStyle(
        1.0f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        "",
        new ConstantColor(0xFFD5DCE7),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false,
        0.0f);

    private final List<String> titles;
    private final ColorValue accentColor;
    private final IntSupplier selectedIndex;
    private final IntConsumer onSelect;
    private final List<LytRect> tabBounds = new ArrayList<>();

    public LytContentTabsHeader(List<String> titles, ColorValue accentColor, IntSupplier selectedIndex,
        IntConsumer onSelect) {
        this.titles = titles;
        this.accentColor = accentColor;
        this.selectedIndex = selectedIndex;
        this.onSelect = onSelect;
    }

    /** Tab hit rects in document coordinates (debug overlay / hit testing). */
    public List<LytRect> getTabBounds() {
        return tabBounds;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        tabBounds.clear();
        int cursorX = x;
        int cursorY = y;
        int rowHeight = 0;
        int bottom = y;
        for (String title : titles) {
            int w = GuideText.measureWidth(title, IDLE_STYLE) + HEADER_PAD_X * 2;
            int h = GuideText.lineHeight(IDLE_STYLE) + HEADER_PAD_TOP + HEADER_PAD_BOTTOM;
            if (cursorX > x && cursorX + w > x + availableWidth) {
                cursorX = x;
                cursorY += rowHeight + HEADER_GAP_Y;
                rowHeight = 0;
            }
            tabBounds.add(new LytRect(cursorX, cursorY, w, h));
            cursorX += w + HEADER_GAP_X;
            rowHeight = Math.max(rowHeight, h);
            bottom = Math.max(bottom, cursorY + h);
        }
        return new LytRect(x, y, availableWidth, bottom - y);
    }

    @Override
    protected void afterExternalLayout() {
        // Recompute tab hit rects from the Rust-computed bounds. The Java
        // layout pre-pass no longer calls computeLayout, so tabBounds must
        // be rebuilt here to stay valid for hit testing and rendering.
        recomputeTabBounds();
    }

    private void recomputeTabBounds() {
        tabBounds.clear();
        int x = bounds.x();
        int y = bounds.y();
        int availableWidth = Math.max(1, bounds.width());
        int cursorX = x;
        int cursorY = y;
        int rowHeight = 0;
        int bottom = y;
        for (String title : titles) {
            int w = GuideText.measureWidth(title, IDLE_STYLE) + HEADER_PAD_X * 2;
            int h = GuideText.lineHeight(IDLE_STYLE) + HEADER_PAD_TOP + HEADER_PAD_BOTTOM;
            if (cursorX > x && cursorX + w > x + availableWidth) {
                cursorX = x;
                cursorY += rowHeight + HEADER_GAP_Y;
                rowHeight = 0;
            }
            tabBounds.add(new LytRect(cursorX, cursorY, w, h));
            cursorX += w + HEADER_GAP_X;
            rowHeight = Math.max(rowHeight, h);
            bottom = Math.max(bottom, cursorY + h);
        }
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (int i = 0; i < tabBounds.size(); i++) {
            tabBounds.set(
                i,
                tabBounds.get(i)
                    .move(deltaX, deltaY));
        }
    }

    @Override
    public int getExplicitHeight() {
        if (titles.isEmpty()) {
            return 0;
        }
        return GuideText.lineHeight(IDLE_STYLE) + HEADER_PAD_TOP + HEADER_PAD_BOTTOM;
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        int sel = Math.clamp(selectedIndex.getAsInt(), 0, Math.max(0, titles.size() - 1));
        int accentArgb = accentColor.resolve(LightDarkMode.current());
        for (int i = 0; i < tabBounds.size(); i++) {
            LytRect tb = tabBounds.get(i);
            boolean selected = i == sel;
            GuideText.emitText(
                c,
                titles.get(i),
                tb.x() + HEADER_PAD_X,
                tb.y() + HEADER_PAD_TOP,
                selected ? SELECTED_STYLE : IDLE_STYLE);
            if (selected) {
                c.emit(
                    new GuideRenderPrimitive.FillRect(
                        tb.x(),
                        tb.bottom() - ACTIVE_RULE_THICKNESS,
                        tb.width(),
                        ACTIVE_RULE_THICKNESS,
                        accentArgb));
            }
        }
    }

    /**
     * Migrated to {@link #computePrimitives}; the legacy path is unreachable
     * (the collector only invokes it when {@link #usePrimitives()} is false).
     */
    @Override
    public void render(RenderContext context) {}

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        for (LytRect tb : tabBounds) {
            if (tb.contains(x, y)) {
                return this;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < tabBounds.size(); i++) {
            if (tabBounds.get(i)
                .contains(x, y)) {
                if (i != selectedIndex.getAsInt()) {
                    onSelect.accept(i);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }
}
