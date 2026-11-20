package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.binding.BindingPresentation;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestCardCompiler.QuestCardPlaceholder;

public class QuestCardScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "QuestCard";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() != EventType.MOUNT) return;
        var placeholder = LytFlowInlineBlock.unwrapPlaceholder(node, QuestCardPlaceholder.class);
        if (placeholder == null) return;
        var key = new BindingKey("betterquesting", placeholder.questId.toString());
        var spec = new BindingSpec(
            key,
            null,
            placeholder.title,
            "native",
            placeholder.showTooltip,
            placeholder.showDesc,
            false,
            false,
            1);
        var display = BindingRuntime
            .describe(key, BindingRuntime.view(placeholder.showTooltip || placeholder.showDesc, false));
        var card = BindingPresentation.block(spec, display, "card", context);
        context.replace(node instanceof LytFlowInlineBlock ? LytFlowInlineBlock.of(card) : card);
    }
}
