package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2CaptureTree.Node;

public class Mui2PageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "mui2_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof IMuiScreen;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        Mui2CaptureTree tree = new Mui2CaptureTree(
            (IMuiScreen) context.capture()
                .screen());
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (Map.Entry<String, Node> entry : tree.nodes()
            .entrySet()) {
            if (entry.getValue()
                .widget() instanceof PagedWidget<?>paged) {
                for (int page = 0; page < paged.getPages()
                    .size(); page++) targets.add(Mui2CaptureTargets.page(entry.getKey(), paged, page));
            } else if (Mui2ScrollViewport.supports(
                entry.getValue()
                    .widget())) {
                        targets.add(
                            Mui2CaptureTargets.scroll(
                                entry.getKey(),
                                entry.getValue()
                                    .widget()));
                    }
        }
        return List.copyOf(targets);
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new PageNavigator(
            (IMuiScreen) context.capture()
                .screen(),
            options.maxTargets());
    }

    public record PageStep(PagedWidget<?> widget, int page) {}

    public record PagePath(PagedWidget<?> widget, List<PageStep> ancestors) {

        public PagePath {
            ancestors = List.copyOf(ancestors);
        }
    }

    public static class PageNavigator extends Mui2PageNavigator {

        public PageNavigator(IMuiScreen screen) {
            super(screen);
        }

        public PageNavigator(IMuiScreen screen, int expansionLimit) {
            super(screen, expansionLimit);
        }
    }
}
