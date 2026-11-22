package com.hfstudio.guidenh.client.capture.render;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTile;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CapturePlacement;

public class CaptureTilePlanner {

    private CaptureTilePlanner() {}

    public static List<CaptureTile> plan(CaptureExportRequest request, CaptureComposition composition,
        CaptureOutputSize output, int tileSize) {
        double scaleX = (double) output.width() / composition.size()
            .width();
        double scaleY = (double) output.height() / composition.size()
            .height();
        List<CaptureTile> tiles = new ArrayList<>();
        for (CapturePlacement placement : composition.placements()) {
            int left = ceiling(
                placement.destinationX(),
                composition.size()
                    .width(),
                output.width());
            int top = ceiling(
                placement.destinationY(),
                composition.size()
                    .height(),
                output.height());
            int right = ceiling(
                placement.destinationX() + placement.width(),
                composition.size()
                    .width(),
                output.width());
            int bottom = ceiling(
                placement.destinationY() + placement.height(),
                composition.size()
                    .height(),
                output.height());
            for (int y = top; y < bottom; y += tileSize) {
                for (int x = left; x < right; x += tileSize) {
                    int width = Math.min(tileSize, right - x);
                    int height = Math.min(tileSize, bottom - y);
                    double sourceX = placement.sourceX() + x / scaleX - placement.destinationX();
                    double sourceY = placement.sourceY() + y / scaleY - placement.destinationY();
                    tiles.add(
                        new CaptureTile(
                            x,
                            y,
                            width,
                            height,
                            new CaptureRect(
                                sourceX / request.viewport()
                                    .scaleX(),
                                sourceY / request.viewport()
                                    .scaleY(),
                                width / scaleX
                                    / request.viewport()
                                        .scaleX(),
                                height / scaleY
                                    / request.viewport()
                                        .scaleY())));
                }
            }
        }
        return List.copyOf(tiles);
    }

    private static int ceiling(int coordinate, int base, int output) {
        return (int) (((long) coordinate * output + base - 1) / base);
    }
}
