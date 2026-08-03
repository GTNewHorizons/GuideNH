package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;

import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.token.DimensionValue;
import com.hfstudio.guidenh.guide.style.token.GuideThemeManager;
import com.hfstudio.guidenh.guide.style.token.TokenKey;
import com.hfstudio.guidenh.guide.style.token.TokenType;

import lombok.Getter;
import lombok.Setter;

public class LytItemImage extends LytBlock implements InteractiveElement {

    public static final int BASE_SIZE = 16;

    /**
     * Optical padding (layout px, applied after scale) reserved on each side of
     * an inline item icon's ink bounds so the visible glyph never touches
     * adjacent text. Only used on the no-label inline path; the label path and
     * the block (non-inline) path keep the legacy 16px cell.
     */
    private static final int INLINE_OPTICAL_PAD = 2;

    /** Theme token: gap between the item icon and its label text. */
    private static final TokenKey<DimensionValue> LABEL_GAP = TokenKey
        .define("--lyt-item-image-label-gap", TokenType.DIMENSION, DimensionValue.px(2));

    private static int labelGap() {
        return GuideThemeManager.instance()
            .active()
            .dim(LABEL_GAP)
            .pxInt();
    }

    private static final int DEFAULT_INLINE_ITEM_VISUAL_Y_OFFSET = 0;

    public static int DEFAULT_TEXT_INLINE_Y_OFFSET = 0;
    public static int DEFAULT_INLINE_Y_OFFSET = 0;

    @Getter
    protected ItemStack stack;
    @Getter
    private float scale = 1f;
    /**
     * -- SETTER --
     * Controls whether hovering over this element shows an item tooltip. Default
     * .
     */
    @Setter
    private boolean showTooltip = true;
    /**
     * -- SETTER --
     * Flag this image as being laid out inline with text. Inline images can be vertically adjusted
     * relative to their centered line position.
     */
    @Getter
    @Setter
    private boolean inline = false;
    @Nullable
    private Integer inlineYOffsetOverride = null;
    @Nullable
    private Integer labelYOffsetOverride = null;
    /**
     * -- SETTER --
     * Controls whether the item icon graphic is rendered. Default
     * .
     */
    @Setter
    private boolean showIcon = true;
    @Nullable
    private String labelPosition = null;
    @Nullable
    private String labelFormat = null;
    private int layoutYOffset = 0;
    /** Label text metrics cached from the last layout pass (used by computePrimitives). */
    private int labelTextW;
    private int labelTextH;
    @Nullable
    private ResolvedTextStyle cachedLabelStyle = null;
    @Nullable
    private String cachedLabelTemplate = null;

    public LytItemImage(ItemStack stack) {
        this.stack = stack;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.125f, scale);
    }

    @Override
    public int getExplicitWidth() {
        if (!showIcon && labelPosition == null) return -1;
        return computeContentSize()[0];
    }

    @Override
    public int getExplicitHeight() {
        if (!showIcon && labelPosition == null) return -1;
        return computeContentSize()[1];
    }

    /** Kept for backward compatibility. Prefer {@link #setShowTooltip(boolean)}. */
    public void setTooltipSuppressed(boolean suppressed) {
        this.showTooltip = !suppressed;
    }

    public boolean isShowingIcon() {
        return showIcon;
    }

    /**
     * Sets the label text position relative to the icon.
     * Accepted values: {@code "left"}, {@code "right"}, or {@code null} for no label.
     */
    public void setLabelPosition(@Nullable String position) {
        this.labelPosition = position;
    }

    /**
     * Sets the label format pattern. Supports Markdown-style wrapping markers and an optional
     * {@code %s} placeholder for the item display name. {@code null} reverts to the default
     * (italic item display name).
     */
    public void setLabelFormat(@Nullable String format) {
        this.labelFormat = format;
        this.cachedLabelStyle = null;
        this.cachedLabelTemplate = null;
    }

    public void setInlineYOffsetOverride(@Nullable Integer override) {
        this.inlineYOffsetOverride = override;
    }

    public int getInlineVerticalOffset() {
        if (!inline) return 0;
        int offset = inlineYOffsetOverride != null ? inlineYOffsetOverride : DEFAULT_INLINE_Y_OFFSET;
        return Math.round(offset * scale);
    }

    /** Returns the item-render-only visual correction for Minecraft's item sprite alignment. */
    public int getInlineVisualYOffset() {
        return inline && showIcon ? Math.round(DEFAULT_INLINE_ITEM_VISUAL_Y_OFFSET * scale) : 0;
    }

    /** Overrides the default inline Y offset for the label text only. Does not affect the icon. */
    public void setLabelYOffsetOverride(@Nullable Integer override) {
        this.labelYOffsetOverride = override;
    }

    /**
     * Computes the full content size of this block (icon + label when both
     * present, icon-only or text-only otherwise). Mirrors computeLayout
     * arithmetic using static GuideText measurement.
     *
     * @return int[]{width, height}
     */
    private int[] computeContentSize() {
        int iconSize = Math.round(BASE_SIZE * scale);
        boolean hasLabel = labelPosition != null && stack != null;

        if (!showIcon && !hasLabel) {
            return new int[]{0, 0};
        }
        if (!hasLabel) {
            // Optical tight advance for inline icons: shrink the cell to the
            // ink width + PAD on each side instead of the full 16px square, so
            // the gap to the following text is consistent across items. Falls
            // back to the legacy 16px cell when ink metrics are unavailable.
            if (inline) {
                IconMetrics m = stack != null ? IconMetrics.forStack(stack) : null;
                if (m != null) {
                    int tightW = Math.round(m.width * scale) + 2 * INLINE_OPTICAL_PAD;
                    return new int[]{tightW, iconSize};
                }
            }
            return new int[]{iconSize, iconSize};
        }

        ResolvedTextStyle textStyle = resolveLabelStyle();
        String text = resolveLabelText();
        int textW = GuideText.measureWidth(text, textStyle);
        int textH = GuideText.lineHeight(textStyle);

        if (!showIcon) {
            return new int[]{textW, textH};
        }

        // showIcon + hasLabel — total width is same for label="left" and label="right"
        int labelYOffset = inline && showIcon
            ? Math.round(
                (labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET) * scale)
            : 0;
        int textTop = (iconSize - textH) / 2 + labelYOffset;
        int top = Math.min(0, textTop);
        int bottom = Math.max(iconSize, textTop + textH);
        int totalW = iconSize + labelGap() + textW;
        int totalH = Math.max(0, bottom - top);
        return new int[]{totalW, totalH};
    }

    /**
     * Computes the inline size (width, height) for serialization when no
     * LayoutContext is available. Delegates to {@link #computeContentSize()}.
     */
    public int[] measureSerializedInlineSize() {
        return computeContentSize();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int iconSize = Math.round(BASE_SIZE * scale);
        int labelYOffset = inline && showIcon
            ? Math.round((labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET) * scale)
            : 0;
        boolean hasLabel = labelPosition != null && stack != null;

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
        labelTextW = textW;
        labelTextH = textH;

        if (!showIcon) {
            layoutYOffset = 0;
            return new LytRect(x, y, textW, textH);
        }
        int textTop = (iconSize - textH) / 2 + labelYOffset;
        int top = Math.min(0, textTop);
        int bottom = Math.max(iconSize, textTop + textH);
        layoutYOffset = top;
        int totalW = iconSize + labelGap() + textW;
        int totalH = Math.max(0, bottom - top);
        return new LytRect(x, y, totalW, totalH);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        if (stack == null || stack.stackSize == 0) return;

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
            // On-the-spot measurement when the layout-pass cache is unpopulated
            // (Java layout pre-pass was removed — computeLayout may not run).
            if (labelTextW <= 0) {
                labelTextW = GuideText.measureWidth(text, textStyle);
                labelTextH = GuideText.lineHeight(textStyle);
            }
            int textVCenter = showIcon ? (iconSize - labelTextH) / 2 : 0;
            int labelYOffset = inline && showIcon
                ? Math
                    .round((labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET) * scale)
                : 0;

            if ("left".equals(labelPosition)) {
                textX = baseX;
                iconX = showIcon ? baseX + labelTextW + labelGap() : baseX;
            } else {
                iconX = baseX;
                textX = showIcon ? baseX + iconSize + labelGap() : baseX;
            }
            textY = baseY + textVCenter + labelYOffset;
            GuideText.emitText(c, text, textX, textY, textStyle);
        }

        if (showIcon) {
            int renderX = iconX;
            if (inline && !hasLabel) {
                // Optical tight placement on the no-label inline path: shift the
                // icon so its ink left edge sits INLINE_OPTICAL_PAD px from the
                // cell's left edge, mirroring the tight advance computed in
                // computeContentSize. Null metrics (atlas not ready / missingno)
                // keep the legacy offset of 0.
                IconMetrics m = stack != null ? IconMetrics.forStack(stack) : null;
                if (m != null) {
                    renderX = iconX - Math.round(m.inkLeft * scale) + INLINE_OPTICAL_PAD;
                }
            }
            int renderY = baseY + getInlineVisualYOffset();
            if (scale == 1f) {
                c.emit(new GuideRenderPrimitive.RenderItem(stack, renderX, renderY));
            } else {
                c.pushTransform(renderX, renderY, scale);
                c.emit(new GuideRenderPrimitive.RenderItem(stack, 0, 0));
                c.popTransform();
            }
        }
    }

    @Override
    public void render(RenderContext context) {
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!showTooltip) return Optional.empty();
        if (stack == null || stack.stackSize == 0) return Optional.empty();
        return Optional.of(new ItemTooltip(stack));
    }

    public List<ItemStack> getStacks() {
        return stack == null ? List.of() : List.of(stack);
    }

    /** Resolves the final label text based on the current stack and format pattern. */
    protected String resolveLabelText() {
        if (stack == null) return "";
        if (labelFormat == null) return stack.getDisplayName();
        if (cachedLabelTemplate == null) {
            cachedLabelTemplate = stripFormatMarkers(labelFormat);
        }
        return cachedLabelTemplate.contains("%s") ? String.format(cachedLabelTemplate, stack.getDisplayName())
            : cachedLabelTemplate;
    }

    /** Resolves the {@link ResolvedTextStyle} for the label based on the format pattern. */
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

    /**
     * Strips all wrapping Markdown-style markers from {@code s}, returning the inner text
     * (which may still contain a {@code %s} placeholder).
     */
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

    /**
     * Builds a {@link TextStyle} by interpreting wrapping Markdown-style markers in {@code format}.
     * Processed markers: {@code **bold**}, {@code *italic*}, {@code _italic_}, {@code ~~strike~~},
     * {@code __underline__}, {@code ++underline++}, {@code ^^wavy^^}, {@code ::dotted::}.
     */
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
