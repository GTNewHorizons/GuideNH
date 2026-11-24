package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuideScreen.NavigationPreview;

public class GuidePageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "guide_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof GuideScreen;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        boolean all = options.scopes()
            .contains(CaptureBatchOptions.REACHABLE);
        if (!all && !options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        GuideScreen screen = (GuideScreen) context.capture()
            .screen();
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (Guide guide : screen.captureGuides(all)) {
            for (ParsedGuidePage page : guide.getPages()) {
                targets.add(
                    new CaptureTargetDescriptor(
                        "guide:" + guide.getId() + "/" + page.getId(),
                        id(),
                        all ? CaptureBatchOptions.REACHABLE : CaptureBatchOptions.PAGES,
                        page.getId()
                            .toString(),
                        screen.canPreviewNavigation() ? "ready" : "unavailable",
                        screen.canPreviewNavigation() ? "" : "Waiting for the guide editor draft",
                        Map.of(
                            "guide",
                            guide.getId()
                                .toString(),
                            "page",
                            page.getId()
                                .toString(),
                            "scroll",
                            "0",
                            "kind",
                            "page")));
            }
        }
        return List.copyOf(targets);
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new GuideNavigator(
            (GuideScreen) context.capture()
                .screen());
    }

    public static class GuideNavigator implements CaptureBatchNavigator {

        private final GuideScreen screen;
        private final NavigationPreview preview;
        private ResourceLocation page;
        private int offset;
        private CaptureTargetDescriptor selected;

        public GuideNavigator(GuideScreen screen) {
            this.screen = screen;
            preview = screen.beginNavigationPreview();
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selected = target;
            page = new ResourceLocation(
                target.data()
                    .get("page"));
            offset = Integer.parseInt(
                target.data()
                    .get("scroll"));
            preview.show(
                new ResourceLocation(
                    target.data()
                        .get("guide")),
                page,
                offset);
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == preview.screen() && preview.ready(page, offset);
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for guide compilation and page layout");
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
            CaptureBatchContext context) {
            if (target.data()
                .get("kind")
                .equals("slice")) return List.of();
            List<CaptureTargetDescriptor> targets = new ArrayList<>();
            int max = preview.maximumScroll();
            int step = preview.scrollStep();
            for (int scroll = step; scroll < max; scroll += step) targets.add(slice(target, scroll));
            if (max > 0) targets.add(slice(target, max));
            return targets;
        }

        private CaptureTargetDescriptor slice(CaptureTargetDescriptor parent, int scroll) {
            return new CaptureTargetDescriptor(
                parent.id() + "/scroll:" + scroll,
                parent.providerId(),
                parent.scope(),
                parent.label() + " [" + scroll + "]",
                "ready",
                "",
                Map.of(
                    "guide",
                    parent.data()
                        .get("guide"),
                    "page",
                    parent.data()
                        .get("page"),
                    "scroll",
                    Integer.toString(scroll),
                    "kind",
                    "slice"));
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            if (selected.data()
                .get("kind")
                .equals("page"))
                return List.of(
                    context.capture()
                        .viewport()
                        .bounds());
            LytRect viewport = preview.viewport();
            return List.of(new CaptureRect(viewport.x(), viewport.y(), viewport.width(), viewport.height()));
        }

        @Override
        public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
            LytRect viewport = preview.viewport();
            return new CaptureScrollGeometry(
                selected.data()
                    .get("guide") + "/"
                    + selected.data()
                        .get("page"),
                new CaptureRect(viewport.x(), viewport.y(), viewport.width(), viewport.height()),
                0,
                offset * preview.zoom(),
                viewport.width(),
                preview.maximumScroll() * preview.zoom() + viewport.height());
        }

        @Override
        public void restore() {
            preview.restore();
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == screen && preview.restored();
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for the original guide view");
        }

        @Override
        public void close() {
            preview.close();
        }
    }
}
