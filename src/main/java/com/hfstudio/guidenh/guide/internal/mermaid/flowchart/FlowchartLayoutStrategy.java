package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.Map;
import java.util.function.Function;

public interface FlowchartLayoutStrategy {

    String getName();

    FlowchartLayoutResult layout(FlowchartDocument document,
        Map<String, FlowchartLayoutResult.NodeMinSize> nodeMinSizes,
        Function<String, FlowchartLayoutResult.LabelSize> measureLabel);

    static FlowchartLayoutStrategy forMode(FlowchartLayoutMode mode) {
        if (mode == null) mode = FlowchartLayoutMode.BUILTIN;
        return switch (mode) {
            case ELK -> new ElkLayoutStrategy();
            case BUILTIN -> new ElkLayoutStrategy();
        };
    }
}
