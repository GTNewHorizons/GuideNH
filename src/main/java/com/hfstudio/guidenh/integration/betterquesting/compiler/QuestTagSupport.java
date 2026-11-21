package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.flow.LytBindingLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class QuestTagSupport {

    private QuestTagSupport() {}

    public static boolean resolveShowTooltip(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        try {
            Boolean value = MdxAttrs.parseOptionalBoolean(el.getAttribute("showTooltip"), "showTooltip");
            return value == null || value;
        } catch (IllegalArgumentException | MdxAttrs.AttributeException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return true;
        }
    }

    public static boolean isNavigable(QuestState state) {
        return state == QuestState.VISIBLE || state == QuestState.COMPLETED || state == QuestState.LOCKED;
    }

    public static boolean isVisibleToPlayer(QuestState state) {
        return state == QuestState.VISIBLE || state == QuestState.COMPLETED;
    }

    public static LytFlowLink createQuestGuiLink(UUID questId, QuestDisplay display, String text, boolean showTooltip) {
        var link = new LytBindingLink(
            BindingSpec.quest(new BindingKey("betterquesting", questId.toString()), text, showTooltip),
            text);
        applyQuestLinkStyle(link, display, text, showTooltip);
        return link;
    }

    public static LytFlowLink createQuestLink(PageCompiler compiler, UUID questId, QuestDisplay display, String text,
        boolean showTooltip) {
        return createQuestGuiLink(questId, display, text, showTooltip);
    }

    public static String nameOrFallback(QuestDisplay display, UUID questId) {
        String name = display.getName();
        return name != null && !name.isEmpty() ? name : "Quest " + questId;
    }

    private static void applyQuestLinkStyle(LytFlowLink link, QuestDisplay display, String text, boolean showTooltip) {
        if (display.getState() == QuestState.COMPLETED) {
            link.modifyStyle(style -> style.color(ColorUtils.MC_GREEN));
        }
        link.appendText(text);
        if (showTooltip) {
            applyTooltip(link, display.getDescription());
        }
    }

    private static void applyTooltip(LytFlowLink link, @Nullable String tooltipText) {
        if (tooltipText == null || tooltipText.isEmpty()) {
            return;
        }
        link.setTooltip(new TextTooltip(tooltipText));
    }
}
