package com.hfstudio.guidenh.client.capture.geometry;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public class CaptureSelectionAreas {

    private CaptureSelectionAreas() {}

    public static List<CaptureRect> visible(List<CaptureRegion> regions) {
        CaptureRect bounds = new CaptureRect(0, 0, 0, 0);
        for (CaptureRegion region : regions) {
            if (!region.erased()) bounds = bounds.union(region.bounds());
        }
        if (bounds.isEmpty()) return List.of();
        List<CaptureRect> visible = List.of(bounds);
        for (CaptureRegion region : regions) {
            if (region.erased()) visible = visible.stream()
                .flatMap(
                    rect -> rect.subtract(region.bounds())
                        .stream())
                .toList();
        }
        return visible;
    }
}
