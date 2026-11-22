package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class VisibleGuiBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "visible";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() != null;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.VISIBLE)) return List.of();
        List<CaptureTarget> windows = windows(context);
        if (options.visibleMode()
            .equals("composite") || windows.isEmpty()) {
            return List.of(
                new CaptureTargetDescriptor(
                    "visible:composite",
                    id(),
                    CaptureBatchOptions.VISIBLE,
                    context.capture()
                        .screen()
                        .getClass()
                        .getSimpleName(),
                    "ready",
                    "",
                    Map.of()));
        }
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (CaptureTarget window : windows) targets.add(
            new CaptureTargetDescriptor(
                "visible:" + window.id(),
                id(),
                CaptureBatchOptions.VISIBLE,
                window.label(),
                "ready",
                "",
                Map.of("window", window.id())));
        return List.copyOf(targets);
    }

    private static List<CaptureTarget> windows(CaptureBatchContext context) {
        Map<CaptureRect, CaptureTarget> windows = new LinkedHashMap<>();
        for (CaptureTarget target : context.geometry()
            .targets()) {
            if (target.kind()
                .equals(CaptureTarget.WINDOW)
                && !target.visibleBounds()
                    .isEmpty())
                windows.put(target.visibleBounds(), target);
        }
        return List.copyOf(windows.values());
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext origin, CaptureBatchOptions options) {
        return new VisibleNavigator(origin);
    }

    public static class VisibleNavigator implements CaptureBatchNavigator {

        private final CaptureBatchContext origin;
        private String selectedWindow = "";

        public VisibleNavigator(CaptureBatchContext origin) {
            this.origin = origin;
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selectedWindow = target.data()
                .getOrDefault("window", "");
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen()
                == origin.capture()
                    .screen()
                && context.capture()
                    .viewport()
                    .equals(
                        origin.capture()
                            .viewport());
            return new CaptureBatchReadiness(
                ready,
                origin.geometry()
                    .revision(),
                ready ? "" : "The visible GUI changed");
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            List<CaptureTarget> windows = windows(origin);
            if (windows.isEmpty()) return List.of(
                origin.capture()
                    .viewport()
                    .bounds());
            return windows.stream()
                .filter(
                    window -> selectedWindow.isEmpty() || window.id()
                        .equals(selectedWindow))
                .map(CaptureTarget::visibleBounds)
                .toList();
        }

        @Override
        public void restore() {}
    }
}
