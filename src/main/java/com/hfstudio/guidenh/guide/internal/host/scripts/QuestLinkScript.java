package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.UUID;

import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.binding.BindingPresentation;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public class QuestLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "QuestLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() != EventType.MOUNT || !(node instanceof LytFlowText placeholder)) return;
        var questId = (UUID) placeholder.getData("questId");
        if (questId == null) return;
        boolean tooltip = Boolean.TRUE.equals(placeholder.getData("showTooltip"));
        var spec = BindingSpec.quest(
            new BindingKey("betterquesting", questId.toString()),
            (String) placeholder.getData("overrideText"),
            tooltip);
        var display = BindingRuntime.describe(spec.key(), BindingRuntime.view(tooltip, false));
        context.replace(BindingPresentation.link(spec, display));
    }
}
