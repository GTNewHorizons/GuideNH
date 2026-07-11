package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytPoint;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightTheme;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Setter;

public class LytCodeBlockToolbar extends LytBox implements InteractiveElement {

    static final GuiSprite COPY_SPRITE = new GuiSprite(
        GuideIconButton.TEX,
        0,
        48,
        16,
        16,
        GuideIconButton.TEXTURE_SIZE,
        GuideIconButton.TEXTURE_SIZE);
    private static final long COPY_TOOLTIP_RESET_DELAY_MILLIS = 1500L;
    private static final int TEXT_CENTERING_OFFSET_Y = 1;
    private static final CodeHighlightTheme CODE_THEME = CodeHighlightTheme.GITHUB_DARK_DEFAULT;
    private static final ConstantColor DEFAULT_TOOLBAR_BACKGROUND = new ConstantColor(
        CODE_THEME.toolbarBackgroundArgb());
    private static final ConstantColor DEFAULT_TOOLBAR_BORDER = new ConstantColor(CODE_THEME.borderArgb());
    private static final ConstantColor DEFAULT_TOOLBAR_TEXT = new ConstantColor(CODE_THEME.toolbarTextArgb());

    private final LytParagraph languageLabel = new LytParagraph();
    private final LytButton copySourceButton;
    private final List<LytButton> extraButtons = new ArrayList<>();

    private ColorValue toolbarBackground = DEFAULT_TOOLBAR_BACKGROUND;
    private ColorValue toolbarBorder = DEFAULT_TOOLBAR_BORDER;
    private ColorValue toolbarText = DEFAULT_TOOLBAR_TEXT;

    private String copyText = "";
    private int preferredWidth;
    @Setter
    private boolean copyButtonVisible = true;

    public LytCodeBlockToolbar() {
        copySourceButton = new LytButton(COPY_SPRITE, new LytSize(16, 16));
        copySourceButton.setColor(toolbarText);
        copySourceButton.setHoverColor(SymbolicColor.ICON_BUTTON_HOVER);
        copySourceButton.setOnClick(screen -> screen.copyCodeBlock(copyText));
        copySourceButton.setTooltipFunction((pressed) -> {
            if (pressed) return GuidebookText.CodeBlockCopySuccess.text();
            return GuidebookText.CodeBlockCopy.text();
        });

        languageLabel.setMarginTop(0);
        languageLabel.setMarginBottom(0);
        languageLabel.modifyStyle(
            style -> style.bold(true)
                .color(toolbarText));
        append(languageLabel);
        append(copySourceButton);
        setPaddingLeft(8);
        setPaddingTop(4);
        setPaddingRight(8);
        setPaddingBottom(4);
        setBorderBottom(new BorderStyle(toolbarBorder, 1));
    }

    public void setLanguageDisplayName(String languageDisplayName) {
        languageLabel.clearContent();
        languageLabel
            .appendText(languageDisplayName != null && !languageDisplayName.isEmpty() ? languageDisplayName : "Text");
    }

    public void setCopyText(String copyText) {
        this.copyText = copyText != null ? copyText : "";
    }

    public void addButton(LytButton button) {
        extraButtons.add(button);
        append(button);
        if (getDocument() != null) getDocument().invalidateLayout();
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }

    public void setToolbarBackground(ColorValue toolbarBackground) {
        this.toolbarBackground = toolbarBackground != null ? toolbarBackground : DEFAULT_TOOLBAR_BACKGROUND;
    }

    public void setToolbarBorder(ColorValue toolbarBorder) {
        this.toolbarBorder = toolbarBorder != null ? toolbarBorder : DEFAULT_TOOLBAR_BORDER;
        setBorderBottom(new BorderStyle(this.toolbarBorder, getBorderBottom().width()));
    }

    public void setToolbarText(ColorValue toolbarText) {
        this.toolbarText = toolbarText != null ? toolbarText : DEFAULT_TOOLBAR_TEXT;
        languageLabel.modifyStyle(style -> style.color(this.toolbarText));
        copySourceButton.setColor(this.toolbarText);
        for (LytButton btn : extraButtons) {
            btn.setColor(this.toolbarText);
        }
    }

    private List<LytButton> visibleButtons() {
        List<LytButton> result = new ArrayList<>(extraButtons.size() + 1);
        result.addAll(extraButtons);
        if (copyButtonVisible) {
            result.add(copySourceButton);
        }
        return result;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int toolbarWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        List<LytButton> buttons = visibleButtons();
        int buttonCount = buttons.size();
        int buttonsWidth = buttonCount * 16 + Math.max(0, buttonCount - 1) * 4;
        int labelWidth = Math.max(1, toolbarWidth - (buttonsWidth > 0 ? buttonsWidth + 8 : 0));
        LytRect labelBounds = languageLabel.layout(context, x, y, labelWidth);
        int height = Math.max(16, labelBounds.height());
        int btnX = x + Math.max(0, toolbarWidth - 16);
        for (int i = buttons.size() - 1; i >= 0; i--) {
            LytButton btn = buttons.get(i);
            LytRect btnBounds = btn.layout(context, btnX, y, 16);
            btn.setLayoutPos(new LytPoint(btnX, y + (height - btnBounds.height()) / 2f));
            height = Math.max(height, btnBounds.height());
            btnX -= 20;
        }
        languageLabel.setLayoutPos(
            new LytPoint(labelBounds.x(), y + (height - labelBounds.height()) / 2f + TEXT_CENTERING_OFFSET_Y));
        return new LytRect(x, y, toolbarWidth, height);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) return false;
        for (LytButton btn : visibleButtons()) {
            if (btn.mouseClicked(screen, x, y, button, doubleClick)) return true;
        }
        return false;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        for (LytButton btn : extraButtons) {
            Optional<GuideTooltip> tip = btn.getTooltip(x, y);
            if (tip.isPresent()) return tip;
        }
        if (copyButtonVisible) {
            return copySourceButton.getTooltip(x, y);
        }
        return Optional.empty();
    }

    @Override
    public void render(RenderContext context) {
        context.fillRect(bounds, toolbarBackground);
        languageLabel.render(context);
        for (LytButton btn : visibleButtons()) {
            btn.render(context);
        }
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            new BorderRenderer()
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
    }
}
