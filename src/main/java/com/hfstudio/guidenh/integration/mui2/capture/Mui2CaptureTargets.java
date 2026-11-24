package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.Map;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class Mui2CaptureTargets {

    private Mui2CaptureTargets() {}

    private static String label(IWidget widget) {
        String name = widget.getName();
        return name == null || name.isBlank() ? widget.getClass()
            .getSimpleName() : name;
    }

    public static CaptureTargetDescriptor page(String path, PagedWidget<?> widget, int page) {
        return new CaptureTargetDescriptor(
            "mui2:page:" + path + "/" + page,
            "mui2_pages",
            CaptureBatchOptions.PAGES,
            label(widget) + " · " + (page + 1),
            "ready",
            "",
            Map.of("path", path, "kind", "page", "page", Integer.toString(page)));
    }

    public static CaptureTargetDescriptor scroll(String path, IWidget widget) {
        return new CaptureTargetDescriptor(
            "mui2:scroll:" + path,
            "mui2_pages",
            CaptureBatchOptions.PAGES,
            label(widget),
            "ready",
            "",
            Map.of("path", path, "kind", "scroll", "page", "0", "x", "0", "y", "0"));
    }
}
