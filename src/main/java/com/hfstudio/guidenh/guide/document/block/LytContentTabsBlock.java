package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

/**
 * Tabbed container: an optional title paragraph, a {@link LytContentTabsHeader}
 * strip, and the ACTIVE tab body. The tree only ever contains those live
 * children — hidden tab bodies are reachable to semantic traversals (search,
 * anchors, resource export) through {@link #visitChildren} but are never laid
 * out or rendered.
 */
public class LytContentTabsBlock extends LytBlock implements InteractiveElement, DebugComponent {

    private static final int ACCENT_WIDTH = 3;
    private static final int CONTAINER_PAD_X = 10;
    private static final int CONTAINER_PAD_Y = 6;
    private static final int TITLE_GAP = 4;
    private static final int BODY_GAP = 6;
    private static final int HEADER_RULE_THICKNESS = 1;
    private static final int HEADER_RULE_COLOR = 0x66586275;
    private static final ConstantColor DEFAULT_ACCENT = new ConstantColor(0xFF7C8795);

    private final List<String> titles = new ArrayList<>();
    private final List<LytBlock> bodies = new ArrayList<>();
    private final ColorValue accentColor;
    @Nullable
    private final LytParagraph titleParagraph;
    private final LytContentTabsHeader headerBlock;
    private int selectedIndex;

    public LytContentTabsBlock(@Nullable String title, @Nullable LytFlowContent icon, int selectedIndex,
        @Nullable ColorValue accentColor, List<ContentTabsSpec.TabEntry> entries) {
        this.accentColor = accentColor != null ? accentColor : DEFAULT_ACCENT;
        this.selectedIndex = Math.max(0, selectedIndex);
        this.titleParagraph = buildTitleParagraph(title, icon);
        if (titleParagraph != null) {
            titleParagraph.parent = this;
            titleParagraph.setMarginBottom(TITLE_GAP);
        }
        for (ContentTabsSpec.TabEntry entry : entries) {
            titles.add(entry.title());
            bodies.add(entry.body());
            entry.body().parent = this;
        }
        headerBlock = new LytContentTabsHeader(titles, this.accentColor, this::getSafeSelectedIndex, this::selectTab);
        headerBlock.parent = this;
        headerBlock.setMarginBottom(BODY_GAP);
        setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setFullWidth(true);
        setBorderLeft(new BorderStyle(this.accentColor, ACCENT_WIDTH));
    }

    @Override
    public List<? extends LytNode> getChildren() {
        // Live tree only: the layout engine and the render collector must not
        // lay out or draw hidden tabs.
        List<LytNode> out = new ArrayList<>();
        if (titleParagraph != null) {
            out.add(titleParagraph);
        }
        out.add(headerBlock);
        if (!bodies.isEmpty()) {
            out.add(activeBody());
        }
        return out;
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        // Semantic traversals (search, anchors, resource export) keep seeing
        // every tab body including hidden ones — pre-migration behavior.
        for (LytNode child : getChildren()) {
            if (child.visit(visitor, includeOutOfTreeContent) == LytVisitor.Result.STOP) {
                return LytVisitor.Result.STOP;
            }
        }
        for (LytBlock body : bodies) {
            if (body != activeBody() && body.visit(visitor, includeOutOfTreeContent) == LytVisitor.Result.STOP) {
                return LytVisitor.Result.STOP;
            }
        }
        return LytVisitor.Result.CONTINUE;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (bodies.isEmpty()) {
            return new LytRect(x, y, 0, 0);
        }
        int contentX = x + ACCENT_WIDTH + CONTAINER_PAD_X;
        int contentY = y + CONTAINER_PAD_Y;
        int contentWidth = Math.max(0, availableWidth - ACCENT_WIDTH - CONTAINER_PAD_X * 2);

        int cursorY = contentY;
        int right = contentX;
        if (titleParagraph != null) {
            LytRect tb = titleParagraph.layout(context, contentX, cursorY, contentWidth);
            cursorY = tb.bottom() + TITLE_GAP;
            right = Math.max(right, tb.right());
        }
        LytRect hb = headerBlock.layout(context, contentX, cursorY, contentWidth);
        cursorY = hb.bottom() + BODY_GAP;
        right = Math.max(right, hb.right());
        LytRect bb = activeBody().layout(context, contentX, cursorY, contentWidth);
        right = Math.max(right, bb.right());

        int contentW = right - contentX;
        int contentH = bb.bottom() - contentY;
        return new LytRect(
            x,
            y,
            Math.max(availableWidth, ACCENT_WIDTH + CONTAINER_PAD_X * 2 + contentW),
            contentH + CONTAINER_PAD_Y * 2);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (LytNode child : getChildren()) {
            if (child instanceof LytBlock b) {
                b.moveLayoutPos(deltaX, deltaY);
            }
        }
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        var bounds = getBounds();
        c.emit(
            new GuideRenderPrimitive.FillRect(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                SymbolicColor.BLOCKQUOTE_BACKGROUND.resolve(LightDarkMode.current())));
        c.emit(
            new GuideRenderPrimitive.FillRect(
                bounds.x(),
                bounds.y(),
                ACCENT_WIDTH,
                bounds.height(),
                accentColor.resolve(LightDarkMode.current())));
        // Panel rule between the header strip and the active body.
        var hb = headerBlock.getBounds();
        if (hb != null && !hb.isEmpty()) {
            float ruleY = hb.bottom() + HEADER_RULE_THICKNESS * 0.5f;
            c.emit(
                new GuideRenderPrimitive.DrawLine(
                    hb.x(),
                    ruleY,
                    hb.right(),
                    ruleY,
                    HEADER_RULE_THICKNESS,
                    HEADER_RULE_COLOR));
        }
    }

    @Override
    public void render(RenderContext context) {}

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        for (LytNode child : getChildren()) {
            LytNode picked = child.pickNode(x, y);
            if (picked != null) {
                return picked;
            }
        }
        return this;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (headerBlock.mouseClicked(screen, x, y, button, doubleClick)) {
            return true;
        }
        LytBlock body = activeBody();
        return body instanceof InteractiveElement interactive
            && interactive.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        LytBlock body = activeBody();
        return body instanceof InteractiveElement interactive ? interactive.getTooltip(x, y) : Optional.empty();
    }

    private void selectTab(int index) {
        if (selectedIndex != index) {
            selectedIndex = index;
            if (getDocument() != null) {
                getDocument().invalidateLayout();
            }
        }
    }

    private LytBlock activeBody() {
        return bodies.get(getSafeSelectedIndex());
    }

    private int getSafeSelectedIndex() {
        if (bodies.isEmpty()) {
            return 0;
        }
        return Math.clamp(selectedIndex, 0, bodies.size() - 1);
    }

    @Nullable
    private LytParagraph buildTitleParagraph(@Nullable String title, @Nullable LytFlowContent icon) {
        boolean hasTitle = title != null && !title.trim()
            .isEmpty();
        if (!hasTitle && icon == null) {
            return null;
        }
        LytParagraph paragraph = new LytParagraph();
        paragraph.setMarginTop(0);
        paragraph.setMarginBottom(0);
        paragraph.modifyStyle(
            style -> style.bold(true)
                .color(accentColor));
        if (icon != null) {
            paragraph.append(icon);
            if (hasTitle) {
                paragraph.appendText(" ");
            }
        }
        if (hasTitle) {
            paragraph.appendText(title.trim());
        }
        return paragraph;
    }

    // Debug implementation

    @Override
    public List<ComponentEntry> getDebugComponents() {
        List<ComponentEntry> components = new ArrayList<>();
        var hb = headerBlock.getBounds();
        if (bodies.isEmpty() || hb == null) {
            return components;
        }
        var tabBounds = headerBlock.getTabBounds();
        for (int i = 0; i < tabBounds.size(); i++) {
            String extra = "Index: " + i;
            if (i == getSafeSelectedIndex()) {
                extra += ", Active";
            }
            int priority = (i == getSafeSelectedIndex()) ? 20 : 15;
            components.add(new SimpleComponentEntry("Tab:" + titles.get(i), tabBounds.get(i), extra, priority));
        }
        return components;
    }
}
