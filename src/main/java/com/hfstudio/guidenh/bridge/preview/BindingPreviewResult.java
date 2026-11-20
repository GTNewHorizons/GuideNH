package com.hfstudio.guidenh.bridge.preview;

import java.util.List;

import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.integration.api.binding.BindingDisplay;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

import lombok.Getter;

@Getter
public class BindingPreviewResult extends PreviewResolveResult {

    private final String type;
    private final String state;
    private final List<BindingKey> prerequisites;

    public BindingPreviewResult(BindingDisplay display, String png, int width, int height) {
        super(
            "bindings",
            display.key()
                .type() + "|"
                + display.key()
                    .id(),
            display.key()
                .id(),
            BindingRuntime.title(display),
            display.description(),
            null,
            null,
            null,
            display.tooltip(),
            png,
            width,
            height);
        type = display.key()
            .type();
        state = display.state()
            .id();
        prerequisites = display.prerequisites();
    }
}
