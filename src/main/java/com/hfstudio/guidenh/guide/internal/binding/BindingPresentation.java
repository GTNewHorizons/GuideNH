package com.hfstudio.guidenh.guide.internal.binding;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.block.LytBindingCard;
import com.hfstudio.guidenh.guide.document.block.LytBindingIcon;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytBindingLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.integration.api.binding.BindingState;

public class BindingPresentation {

    protected BindingPresentation() {}

    public static LytFlowSpan link(BindingSpec spec, BindingDisplay display) {
        boolean restricted = restricted(display);
        String text = restricted ? BindingRuntime.title(display)
            : spec.text() != null ? spec.text() : spec.title() != null ? spec.title() : BindingRuntime.title(display);
        LytFlowSpan span = restricted ? new LytFlowSpan() : new LytBindingLink(spec, text);
        span.appendText(text);
        if (display.state()
            .equals(BindingState.COMPLETED)) span.modifyStyle(style -> style.color(ColorUtils.MC_GREEN));
        if (restricted) span.modifyStyle(
            style -> style.color(
                display.state()
                    .equals(BindingState.MISSING) ? ColorUtils.MC_RED : ColorUtils.MC_DARK_GRAY)
                .italic(true));
        if (spec.showTooltip() && span instanceof LytBindingLink link
            && !display.tooltip()
                .isEmpty())
            link.setTooltip(new TextTooltip(String.join("\n", display.tooltip())));
        return span;
    }

    public static boolean showDescription(BindingDisplay display) {
        return !restricted(display);
    }

    public static boolean restricted(BindingDisplay display) {
        return display.state()
            .equals(BindingState.HIDDEN)
            || display.state()
                .equals(BindingState.MISSING)
            || display.state()
                .equals(BindingState.UNAVAILABLE);
    }

    public static LytBlock block(BindingSpec spec, BindingDisplay display, String kind, ScriptContext context) {
        if ("icon".equals(kind)) {
            LytBlock icon = icon(spec, display, context);
            if (icon != null) return icon;
            var fallback = new LytParagraph();
            fallback.append(link(spec, display));
            return fallback;
        }
        var box = new LytBindingCard(spec);
        box.setQuoteStyle(
            display.state()
                .equals(BindingState.COMPLETED) ? ColorUtils.MC_GREEN
                    : display.state()
                        .equals(BindingState.MISSING) ? ColorUtils.MC_RED
                            : restricted(display) || display.state()
                                .equals(BindingState.LOCKED) ? ColorUtils.MC_GRAY : ColorUtils.LINK,
            null,
            null);
        var title = new LytParagraph();
        title.setMarginTop(0);
        title.setMarginBottom(2);
        if (spec.showIcon()) {
            var icon = icon(spec, display, context);
            if (icon != null) {
                title.append(LytFlowInlineBlock.of(icon));
                title.appendText(" ");
            }
        }
        title.append(link(spec, display));
        box.append(title);
        if (showDescription(display) && spec.showDescription()
            && display.description() != null
            && !display.description()
                .isEmpty()) {
            var description = new LytParagraph();
            description.appendText(display.description());
            box.append(description);
        }
        if (!restricted(display) && spec.showParents()) {
            for (var parent : display.prerequisites()) {
                var parentDisplay = BindingRuntime.describe(parent, BindingRuntime.view(false, false));
                if (restricted(parentDisplay)) continue;
                var paragraph = new LytParagraph();
                paragraph.append(
                    link(
                        new BindingSpec(parent, null, null, "auto", spec.showTooltip(), false, false, false, 1),
                        parentDisplay));
                box.append(paragraph);
            }
        }
        return box;
    }

    private static LytBlock icon(BindingSpec spec, BindingDisplay display, ScriptContext context) {
        var icon = display.icon();
        if (restricted(display) || icon == null) return null;
        if (icon.itemId() != null) {
            var stack = GuideDisplayItemStacks.resolveItemStack(
                icon.itemId() + ":" + icon.meta() + (icon.nbt() == null ? "" : icon.nbt()),
                "minecraft");
            if (stack == null) return null;
            var image = new LytItemImage(stack);
            image.setScale(spec.scale());
            image.setTooltipSuppressed(true);
            return new LytBindingIcon(spec, display, image);
        }
        if (icon.texture() == null) return null;
        var image = new LytImage();
        image.setTexture(
            icon.texture(),
            GuidePageTexture.loadCached(icon.texture(), () -> context.loadAsset(icon.texture())));
        image.setDisplaySize(Math.max(1, Math.round(16 * spec.scale())), Math.max(1, Math.round(16 * spec.scale())));
        image.setAlt(BindingRuntime.title(display));
        return new LytBindingIcon(spec, display, image);
    }
}
