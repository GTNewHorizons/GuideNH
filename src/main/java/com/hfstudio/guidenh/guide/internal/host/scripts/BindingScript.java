package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.binding.BindingPlaceholder;
import com.hfstudio.guidenh.guide.internal.binding.BindingPresentation;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class BindingScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Binding";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() != EventType.MOUNT) return;
        var placeholder = LytFlowInlineBlock.unwrapPlaceholder(node, BindingPlaceholder.class);
        if (placeholder != null) {
            var spec = placeholder.getSpec();
            var display = BindingRuntime
                .describe(spec.key(), BindingRuntime.view(spec.showDescription() || spec.showTooltip(), false));
            var block = BindingPresentation.block(spec, display, placeholder.getKind(), context);
            context.replace(node instanceof LytFlowInlineBlock ? LytFlowInlineBlock.of(block) : block);
        } else if (node instanceof LytFlowContent flow && flow.getData("bindingSpec") instanceof BindingSpec spec) {
            context.replace(
                BindingPresentation
                    .link(spec, BindingRuntime.describe(spec.key(), BindingRuntime.view(spec.showTooltip(), false))));
        }
    }
}
