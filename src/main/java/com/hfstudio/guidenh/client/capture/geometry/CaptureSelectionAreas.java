package com.hfstudio.guidenh.client.capture.geometry;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public class CaptureSelectionAreas {

    private CaptureSelectionAreas() {
    }

    public static List<CaptureRegion> preview(List<CaptureRegion> regions, CaptureRect candidate) {
        if (candidate == null || candidate.isEmpty()) return regions;
        List<CaptureRegion> preview = new ArrayList<>(regions.size() + 1);
        preview.addAll(regions);
        // This view-only region never enters selection history or export requests.
        preview.add(new CaptureRegion(Long.MIN_VALUE, candidate, "", false, false));
        return preview;
    }

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
