package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.block.LytFluidImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytFluidLink;
import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.fluid.FluidGuideTargetResolver;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class FluidLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "FluidLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() != EventType.MOUNT || !(node instanceof LytFluidLink link)) return;
        if (Boolean.TRUE.equals(link.getData("mounted"))) {
            context.replace(link);
            return;
        }
        try {
            FluidStack stack = FluidStacks.resolve(link.getFluidSpec());
            boolean showTooltip = Boolean.TRUE.equals(link.getData("showTooltip"));
            String page = (String) link.getData("pageId");
            String guide = (String) link.getData("guideId");
            String override = (String) link.getData("linksTo");
            GuideAnchor target = override == null || override.trim()
                .isEmpty() ? FluidGuideTargetResolver.resolve(
                    link.getFluidSpec()
                        .id(),
                    GuideRegistry.getAll()) : GuideLinkTargetResolver.resolve(override, page, guide, context);
            if (showTooltip) link.setTooltip(new FluidTooltip(link.getFluidSpec(), stack));
            if (target != null && !(target.page()
                .anchor() == null && target.page()
                    .pageId()
                    .toString()
                    .equals(page)
                && target.guideId()
                    .toString()
                    .equals(guide))) {
                link.setGuideLink(target.guideId(), target.page());
            } else {
                link.modifyStyle(
                    style -> style.color(ColorUtils.MC_GRAY)
                        .italic(true));
                link.modifyHoverStyle(style -> style.underlined(false));
            }
            String iconPosition = (String) link.getData("showIcon");
            if (Boolean.TRUE.equals(link.getData("showText")) && !hasText(link))
                link.appendText(FluidStacks.displayName(stack));
            if (iconPosition != null) {
                LytFluidImage image = new LytFluidImage(link.getFluidSpec());
                image.setInline(true);
                image.setScale((Float) link.getData("scale"));
                image.setShowTooltip(showTooltip);
                var icon = LytFlowInlineBlock.of(image);
                if ("left".equals(iconPosition)) {
                    link.getChildren()
                        .addFirst(icon);
                    icon.setParent(link);
                } else link.append(icon);
            }
            link.setData("mounted", true);
            context.replace(link);
        } catch (IllegalArgumentException exception) {
            context.replace(LytFlowInlineBlock.of(LytParagraph.error("[FluidLink] " + exception.getMessage())));
        }
    }

    private static boolean hasText(LytFlowSpan span) {
        for (LytFlowContent child : span.getChildren()) {
            if (child instanceof LytFlowText text && !text.getText()
                .isEmpty()) return true;
            if (child instanceof LytFlowSpan nested && hasText(nested)) return true;
        }
        return false;
    }
}
