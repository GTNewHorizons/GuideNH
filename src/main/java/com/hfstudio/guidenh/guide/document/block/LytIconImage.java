package com.hfstudio.guidenh.guide.document.block;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;

import lombok.Getter;
import lombok.Setter;

public abstract class LytIconImage extends LytBlock implements InteractiveElement {

    public static final int BASE_SIZE = 16;

    private static final int LABEL_GAP = 2;
    private static final int DEFAULT_INLINE_ITEM_VISUAL_Y_OFFSET = 0;

    public static int DEFAULT_TEXT_INLINE_Y_OFFSET = 0;
    public static int DEFAULT_INLINE_Y_OFFSET = 0;

    @Getter
    private float scale = 1f;
    @Getter
    @Setter
    private boolean showTooltip = true;
    @Getter
    @Setter
    private boolean inline = false;
    @Nullable
    @Getter
    private Integer inlineYOffsetOverride = null;
    @Nullable
    @Getter
    private Integer labelYOffsetOverride = null;
    @Setter
    private boolean showIcon = true;
    @Nullable
    @Getter
    private String labelPosition = null;
    @Nullable
    @Getter
    private String labelFormat = null;
    private int layoutYOffset = 0;
    @Nullable
    private ResolvedTextStyle cachedLabelStyle = null;
    @Nullable
    private String cachedLabelTemplate = null;
    private String cachedDisplayName;
    private String cachedFormattedLabel;

    protected abstract String getDisplayName();

    protected abstract boolean hasContent();

    protected abstract void renderIcon(RenderContext context, int x, int y);

    protected boolean isRenderable() {
        return hasContent();
    }

    protected boolean isTooltipVisible() {
        return showTooltip;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.125f, scale);
    }

    public void setTooltipSuppressed(boolean suppressed) {
        this.showTooltip = !suppressed;
    }

    public boolean isShowingIcon() {
        return showIcon;
    }

    public void setLabelPosition(@Nullable String position) {
        this.labelPosition = position;
    }

    public void setLabelFormat(@Nullable String format) {
        this.labelFormat = format;
        this.cachedLabelStyle = null;
        this.cachedLabelTemplate = null;
        this.cachedFormattedLabel = null;
    }

    public void setInlineYOffsetOverride(@Nullable Integer override) {
        this.inlineYOffsetOverride = override;
    }

    public int getInlineVerticalOffset() {
        if (!inline) return 0;
        int offset = inlineYOffsetOverride != null ? inlineYOffsetOverride : DEFAULT_INLINE_Y_OFFSET;
        return Math.round(offset * scale);
    }

    public int getInlineVisualYOffset() {
        return inline && showIcon ? Math.round(DEFAULT_INLINE_ITEM_VISUAL_Y_OFFSET * scale) : 0;
    }

    public void setLabelYOffsetOverride(@Nullable Integer override) {
        this.labelYOffsetOverride = override;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int iconSize = Math.round(BASE_SIZE * scale);
        int labelYOffset = inline && showIcon
            ? Math.round((labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET) * scale)
            : 0;
        boolean hasLabel = labelPosition != null && hasContent();

        if (!showIcon && !hasLabel) {
            layoutYOffset = 0;
            return new LytRect(x, y, 0, 0);
        }
        if (!hasLabel) {
            layoutYOffset = 0;
            return new LytRect(x, y, iconSize, iconSize);
        }

        ResolvedTextStyle textStyle = resolveLabelStyle();
        String text = resolveLabelText();
        int textW = measureTextWidth(context, text, textStyle);
        int textH = context.getLineHeight(textStyle);

        if (!showIcon) {
            layoutYOffset = 0;
            return new LytRect(x, y, textW, textH);
        }
        int textTop = (iconSize - textH) / 2 + labelYOffset;
        int top = Math.min(0, textTop);
        int bottom = Math.max(iconSize, textTop + textH);
        layoutYOffset = top;
        int totalW = iconSize + LABEL_GAP + textW;
        int totalH = Math.max(0, bottom - top);
        return new LytRect(x, y, totalW, totalH);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (!isRenderable()) return;

        int baseX = bounds.x();
        int baseY = bounds.y() - layoutYOffset;
        int iconSize = Math.round(BASE_SIZE * scale);
        boolean hasLabel = labelPosition != null;

        int iconX = baseX;
        int textX = baseX;
        int textY = baseY;

        if (hasLabel) {
            ResolvedTextStyle textStyle = resolveLabelStyle();
            String text = resolveLabelText();
            int textW = context.getStringWidth(text, textStyle);
            int textH = context.getLineHeight(textStyle);
            int textVCenter = showIcon ? (iconSize - textH) / 2 : 0;
            int labelYOffset = inline && showIcon
                ? Math
                    .round((labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET) * scale)
                : 0;

            if ("left".equals(labelPosition)) {
                textX = baseX;
                iconX = showIcon ? baseX + textW + LABEL_GAP : baseX;
            } else {
                iconX = baseX;
                textX = showIcon ? baseX + iconSize + LABEL_GAP : baseX;
            }
            textY = baseY + textVCenter + labelYOffset;
            context.drawText(text, textX, textY, textStyle);
        }

        if (showIcon) {
            int renderX = iconX;
            int renderY = baseY + getInlineVisualYOffset();
            renderIcon(context, renderX, renderY);
        }
    }

    protected String resolveLabelText() {
        if (!hasContent()) return "";
        if (labelFormat == null) return getDisplayName();
        if (cachedLabelTemplate == null) {
            cachedLabelTemplate = stripFormatMarkers(labelFormat);
        }
        if (!cachedLabelTemplate.contains("%s")) return cachedLabelTemplate;
        String name = getDisplayName();
        if (cachedFormattedLabel == null || !Objects.equals(name, cachedDisplayName)) {
            cachedDisplayName = name;
            cachedFormattedLabel = cachedLabelTemplate.replace("%s", name);
        }
        return cachedFormattedLabel;
    }

    protected ResolvedTextStyle resolveLabelStyle() {
        if (cachedLabelStyle == null) {
            cachedLabelStyle = labelFormat == null ? TextStyle.builder()
                .italic(true)
                .build()
                .mergeWith(DefaultStyles.BASE_STYLE)
                : buildFormatStyle(labelFormat).mergeWith(DefaultStyles.BASE_STYLE);
        }
        return cachedLabelStyle;
    }

    private int measureTextWidth(LayoutContext context, String text, ResolvedTextStyle style) {
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            width += context.getAdvance(cp, style);
            offset += Character.charCount(cp);
        }
        return Math.round(width);
    }

    private static boolean isWrapped(String s, String marker) {
        return s.length() > 2 * marker.length() && s.startsWith(marker) && s.endsWith(marker);
    }

    private static String stripFormatMarkers(String s) {
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isWrapped(s, "~~")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "**")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "__")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "^^")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "::")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "++")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "*")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            } else if (isWrapped(s, "_")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return s;
    }

    private static TextStyle buildFormatStyle(String s) {
        TextStyle.Builder builder = TextStyle.builder();
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isWrapped(s, "~~")) {
                builder.strikethrough(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "**")) {
                builder.bold(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "__")) {
                builder.underlined(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "^^")) {
                builder.wavyUnderline(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "::")) {
                builder.dottedUnderline(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "++")) {
                builder.underlined(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "*")) {
                builder.italic(true);
                s = s.substring(1, s.length() - 1);
                changed = true;
            } else if (isWrapped(s, "_")) {
                builder.italic(true);
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return builder.build();
    }
}
