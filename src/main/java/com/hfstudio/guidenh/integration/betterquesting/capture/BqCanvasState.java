package com.hfstudio.guidenh.integration.betterquesting.capture;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.integration.betterquesting.capture.BqCaptureCanvasTree.Node;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.panels.lists.CanvasHoverTray;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;

public class BqCanvasState {

    private final Map<String, Scroll> scrolls;
    private final Map<String, Boolean> trays;

    public record Scroll(int x, int y, float zoom) {

        public static Scroll from(CanvasScrolling canvas) {
            return new Scroll(canvas.getScrollX(), canvas.getScrollY(), canvas.getZoom());
        }

        public void apply(CanvasScrolling canvas) {
            canvas.setZoom(zoom);
            canvas.setScrollX(x);
            canvas.setScrollY(y);
            canvas.updatePanelScroll();
        }
    }

    public BqCanvasState(GuiScreenCanvas screen) {
        Map<String, Scroll> scrolls = new LinkedHashMap<>();
        Map<String, Boolean> trays = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : nodes(screen).entrySet()) {
            if (entry.getValue()
                .panel() instanceof CanvasScrolling canvas) scrolls.put(entry.getKey(), Scroll.from(canvas));
            if (entry.getValue()
                .panel() instanceof CanvasHoverTray tray) trays.put(entry.getKey(), tray.isTrayOpen());
        }
        this.scrolls = Collections.unmodifiableMap(scrolls);
        this.trays = Collections.unmodifiableMap(trays);
    }

    private static Map<String, Node> nodes(GuiScreenCanvas screen) {
        return BqCaptureCanvasTree.collect(screen, new CaptureRect(0, 0, screen.width, screen.height), true);
    }

    public void restore(GuiScreenCanvas screen) {
        Map<String, Node> nodes = nodes(screen);
        for (Map.Entry<String, Boolean> entry : trays.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.panel() instanceof CanvasHoverTray tray))
                throw new IllegalStateException("The original BQ tray disappeared");
            tray.setTrayState(entry.getValue(), 0);
        }
        nodes = nodes(screen);
        for (Map.Entry<String, Scroll> entry : scrolls.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.panel() instanceof CanvasScrolling canvas))
                throw new IllegalStateException("The original BQ scroll canvas disappeared");
            entry.getValue()
                .apply(canvas);
        }
    }

    public boolean restored(GuiScreenCanvas screen) {
        Map<String, Node> nodes = nodes(screen);
        for (Map.Entry<String, Boolean> entry : trays.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.panel() instanceof CanvasHoverTray tray)
                || tray.isTrayOpen() != entry.getValue()) return false;
        }
        for (Map.Entry<String, Scroll> entry : scrolls.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.panel() instanceof CanvasScrolling canvas)
                || !entry.getValue()
                    .equals(Scroll.from(canvas)))
                return false;
        }
        return true;
    }
}
