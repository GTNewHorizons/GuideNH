package com.hfstudio.guidenh.guide.document.flow;

import com.hfstudio.guidenh.guide.internal.binding.BindingNavigationService;
import com.hfstudio.guidenh.guide.internal.binding.BindingSpec;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytBindingLink extends LytFlowLink {

    private final BindingSpec spec;
    private final String title;

    public LytBindingLink(BindingSpec spec, String title) {
        this.spec = spec;
        this.title = title;
        setClickCallback(host -> {
            if ("native".equals(spec.target())) BindingNavigationService.openNative(host, spec.key());
            else if ("guide".equals(spec.target())) BindingNavigationService.openGuidesOnly(host, spec.key(), title);
            else BindingNavigationService.openGuidesOrNative(host, spec.key(), title);
        });
    }

    public BindingSpec getBindingSpec() {
        return spec;
    }

    public String getBindingTitle() {
        return title;
    }

    @Override
    public boolean mouseClicked(GuideUiHost host, int x, int y, int button, boolean doubleClick) {
        if (button == 1) {
            BindingNavigationService.openGuidesOrNative(host, spec.key(), title);
            return true;
        }
        return super.mouseClicked(host, x, y, button, doubleClick);
    }
}
