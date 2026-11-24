package com.hfstudio.guidenh.client.capture.export;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public record CaptureComposition(CaptureOutputSize size, List<CapturePlacement> placements,
    List<CapturePlacement> regions) {

    public CaptureComposition(CaptureOutputSize size, List<CapturePlacement> placements) {
        this(size, placements, placements);
    }

    public CaptureComposition {
        placements = List.copyOf(placements);
        regions = List.copyOf(regions);
    }

    public List<CapturePlacement> visiblePlacements() {
        TreeSet<Integer> boundaries = new TreeSet<>();
        for (CapturePlacement placement : placements) {
            boundaries.add(placement.destinationY());
            boundaries.add(placement.destinationY() + placement.height());
        }
        List<Integer> ys = List.copyOf(boundaries);
        List<CapturePlacement> visible = new ArrayList<>();
        for (int index = 1; index < ys.size(); index++) {
            int top = ys.get(index - 1);
            int bottom = ys.get(index);
            List<CapturePlacement> row = placements.stream()
                .filter(p -> p.destinationY() <= top && p.destinationY() + p.height() >= bottom)
                .sorted(Comparator.comparingInt(CapturePlacement::destinationX))
                .toList();
            int covered = 0;
            for (CapturePlacement placement : row) {
                int left = Math.max(covered, placement.destinationX());
                int right = placement.destinationX() + placement.width();
                if (left < right) visible.add(
                    new CapturePlacement(
                        placement.sourceX() + left - placement.destinationX(),
                        placement.sourceY() + top - placement.destinationY(),
                        right - left,
                        bottom - top,
                        left,
                        top));
                covered = Math.max(covered, right);
            }
        }
        return List.copyOf(visible);
    }

    public List<CapturePlacement> visiblePlacements(List<CaptureRect> erased) {
        List<CapturePlacement> visible = new ArrayList<>();
        for (CapturePlacement placement : visiblePlacements()) {
            List<CaptureRect> pieces = List
                .of(new CaptureRect(placement.sourceX(), placement.sourceY(), placement.width(), placement.height()));
            for (CaptureRect mask : erased) pieces = pieces.stream()
                .flatMap(
                    piece -> piece.subtract(mask)
                        .stream())
                .toList();
            for (CaptureRect piece : pieces) visible.add(
                new CapturePlacement(
                    (int) piece.x(),
                    (int) piece.y(),
                    (int) piece.width(),
                    (int) piece.height(),
                    placement.destinationX() + (int) piece.x() - placement.sourceX(),
                    placement.destinationY() + (int) piece.y() - placement.sourceY()));
        }
        return List.copyOf(visible);
    }
}
