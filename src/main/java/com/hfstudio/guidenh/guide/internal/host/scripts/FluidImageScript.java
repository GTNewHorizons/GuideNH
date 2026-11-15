package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.compiler.tags.FluidImageCompiler.FluidImagePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class FluidImageScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "FluidImage";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext context) {
        if (event.type() != EventType.MOUNT) return;
        FluidImagePlaceholder placeholder = LytFlowInlineBlock.unwrapPlaceholder(node, FluidImagePlaceholder.class);
        if (placeholder == null) return;
        try {
            context.replace(
                placeholder.getOptions()
                    .createImage());
        } catch (IllegalArgumentException exception) {
            context.replace(LytParagraph.error("[FluidImage] " + exception.getMessage()));
        }
    }
}
