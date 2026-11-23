package com.hfstudio.guidenh.client.capture.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public class CaptureEraseMask {

    private final List<CaptureRect> rectangles;

    public CaptureEraseMask(CaptureExportRequest request, CaptureComposition composition, CaptureOutputSize output) {
        List<CaptureRect> masks = new ArrayList<>();
        for (CaptureRegion region : request.regions()) {
            if (!region.erased()) continue;
            CaptureRect erase = request.viewport()
                .pixels(region.bounds());
            for (CapturePlacement placement : composition.placements()) {
                CaptureRect clip = erase.intersect(
                    new CaptureRect(placement.sourceX(), placement.sourceY(), placement.width(), placement.height()));
                if (clip.isEmpty()) continue;
                int left = ceiling(
                    (int) clip.x() - placement.sourceX() + placement.destinationX(),
                    composition.size()
                        .width(),
                    output.width());
                int right = ceiling(
                    (int) clip.right() - placement.sourceX() + placement.destinationX(),
                    composition.size()
                        .width(),
                    output.width());
                int top = ceiling(
                    (int) clip.y() - placement.sourceY() + placement.destinationY(),
                    composition.size()
                        .height(),
                    output.height());
                int bottom = ceiling(
                    (int) clip.bottom() - placement.sourceY() + placement.destinationY(),
                    composition.size()
                        .height(),
                    output.height());
                masks.add(new CaptureRect(left, top, right - left, bottom - top));
            }
        }
        rectangles = List.copyOf(masks);
    }

    public void apply(int y, int[] row, int background) {
        for (CaptureRect rectangle : rectangles) {
            if (y >= rectangle.y() && y < rectangle.bottom())
                Arrays.fill(row, (int) rectangle.x(), (int) rectangle.right(), background);
        }
    }

    private static int ceiling(int coordinate, int base, int output) {
        return (int) (((long) coordinate * output + base - 1) / base);
    }
}
