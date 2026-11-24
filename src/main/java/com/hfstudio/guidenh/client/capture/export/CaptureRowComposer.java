package com.hfstudio.guidenh.client.capture.export;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureLayoutOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class CaptureRowComposer implements CaptureRows {

    private final CaptureFrame frame;
    private final CaptureComposition composition;
    private final CaptureOutputSize outputSize;
    private final int background;
    private final CaptureEraseMask mask;

    public CaptureRowComposer(CaptureExportRequest request, CaptureFrame frame) {
        this.frame = frame;
        composition = compose(request);
        outputSize = request.options()
            .resolution()
            .resolve(
                composition.size()
                    .width(),
                composition.size()
                    .height());
        background = request.options()
            .content()
            .backgroundArgb();
        mask = new CaptureEraseMask(request, composition, outputSize);
    }

    public CaptureOutputSize size() {
        return outputSize;
    }

    public CaptureComposition composition() {
        return composition;
    }

    public static CaptureComposition compose(CaptureExportRequest request) {
        List<CaptureRect> crops = request.regions()
            .stream()
            .filter(region -> !region.erased())
            .map(
                region -> request.viewport()
                    .pixels(region.bounds()))
            .filter(rectangle -> !rectangle.isEmpty())
            .toList();
        if (crops.isEmpty()) throw new IllegalArgumentException("The selected areas are outside the GUI");
        CaptureLayoutOptions layout = request.options()
            .layout();
        List<CapturePlacement> placements = new ArrayList<>(crops.size());
        if (layout.arrangement()
            .equals("original")) {
            CaptureRect bounds = crops.getFirst();
            for (int index = 1; index < crops.size(); index++) bounds = bounds.union(crops.get(index));
            for (CaptureRect crop : crops)
                placements.add(placement(crop, (int) (crop.x() - bounds.x()), (int) (crop.y() - bounds.y())));
            return new CaptureComposition(
                new CaptureOutputSize((int) bounds.width(), (int) bounds.height()),
                placements,
                placements);
        }
        int columns = switch (layout.arrangement()) {
            case "horizontal" -> crops.size();
            case "vertical" -> 1;
            case "grid" -> Math.min(layout.columns(), crops.size());
            default -> throw new IllegalArgumentException("Unknown capture arrangement: " + layout.arrangement());
        };
        int rows = (crops.size() + columns - 1) / columns;
        int[] widths = new int[columns];
        int[] heights = new int[rows];
        for (int index = 0; index < crops.size(); index++) {
            widths[index % columns] = Math.max(
                widths[index % columns],
                (int) crops.get(index)
                    .width());
            heights[index / columns] = Math.max(
                heights[index / columns],
                (int) crops.get(index)
                    .height());
        }
        int[] xs = offsets(widths, layout.spacing());
        int[] ys = offsets(heights, layout.spacing());
        for (int index = 0; index < crops.size(); index++) {
            placements.add(placement(crops.get(index), xs[index % columns], ys[index / columns]));
        }
        return new CaptureComposition(
            new CaptureOutputSize(
                Math.addExact(xs[columns - 1], widths[columns - 1]),
                Math.addExact(ys[rows - 1], heights[rows - 1])),
            placements);
    }

    private static int[] offsets(int[] sizes, int spacing) {
        int[] result = new int[sizes.length];
        for (int index = 1; index < result.length; index++) {
            result[index] = Math.addExact(result[index - 1], Math.addExact(sizes[index - 1], spacing));
        }
        return result;
    }

    private static CapturePlacement placement(CaptureRect crop, int x, int y) {
        return new CapturePlacement((int) crop.x(), (int) crop.y(), (int) crop.width(), (int) crop.height(), x, y);
    }

    public void row(int y, int[] target) {
        Arrays.fill(target, background);
        int baseY = (int) ((long) y * composition.size()
            .height() / outputSize.height());
        for (CapturePlacement placement : composition.placements()) {
            int sourceY = baseY - placement.destinationY();
            if (sourceY < 0 || sourceY >= placement.height()) continue;
            int start = scaledCeiling(
                placement.destinationX(),
                composition.size()
                    .width(),
                outputSize.width());
            int end = scaledCeiling(
                placement.destinationX() + placement.width(),
                composition.size()
                    .width(),
                outputSize.width());
            for (int x = start; x < end; x++) {
                int sourceX = (int) ((long) x * composition.size()
                    .width() / outputSize.width()) - placement.destinationX();
                int pixel = frame.argb(placement.sourceX() + sourceX, placement.sourceY() + sourceY);
                target[x] = composite(pixel, background);
            }
        }
        mask.apply(y, target, background);
    }

    private static int scaledCeiling(int coordinate, int base, int scaled) {
        return (int) (((long) coordinate * scaled + base - 1) / base);
    }

    public static int composite(int foreground, int background) {
        int alpha = foreground >>> 24;
        int backdropAlpha = background >>> 24;
        if (alpha == 255 || backdropAlpha == 0) return foreground;
        if (alpha == 0) return background;
        int remaining = backdropAlpha * (255 - alpha) / 255;
        int combined = alpha + remaining;
        int red = (((foreground >>> 16) & 255) * alpha + ((background >>> 16) & 255) * remaining) / combined;
        int green = (((foreground >>> 8) & 255) * alpha + ((background >>> 8) & 255) * remaining) / combined;
        int blue = ((foreground & 255) * alpha + (background & 255) * remaining) / combined;
        return combined << 24 | red << 16 | green << 8 | blue;
    }

    public BufferedImage toImage(long memoryBudget) {
        if (outputSize.rgbaBytes() > memoryBudget) {
            throw new IllegalArgumentException(
                "This output exceeds the animation memory budget; use PNG or a smaller area");
        }
        BufferedImage image = new BufferedImage(outputSize.width(), outputSize.height(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();
        int[] row = new int[outputSize.width()];
        for (int y = 0; y < outputSize.height(); y++) {
            row(y, row);
            System.arraycopy(row, 0, pixels, y * outputSize.width(), row.length);
        }
        return image;
    }
}
