package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;
import com.hfstudio.guidenh.client.capture.render.CaptureTileStore;

public class CaptureScrollRows implements CaptureRows {

    private static final int BAND = 1024;
    private final CaptureOutputSize size;
    private final int background;
    private final List<Placement> placements;
    private final Map<Integer, List<Placement>> bands = new HashMap<>();
    private final ByteBuffer buffer;
    private final int[] sampled;

    public record Placement(CaptureTileStore source, CaptureRect destination) {}

    public CaptureScrollRows(CaptureOutputSize size, int background, List<Placement> placements, long memoryBudget) {
        this.size = size;
        this.background = background;
        this.placements = List.copyOf(placements);
        int width = placements.stream()
            .mapToInt(
                p -> p.source()
                    .size()
                    .width())
            .max()
            .orElse(1);
        if (width * 8L + size.width() * 12L > memoryBudget)
            throw new IllegalArgumentException("Scroll scanlines exceed the composition memory budget");
        buffer = ByteBuffer.allocate(Math.multiplyExact(width, 4))
            .order(ByteOrder.LITTLE_ENDIAN);
        sampled = new int[width];
        for (Placement placement : placements) {
            int first = (int) placement.destination()
                .y() / BAND;
            int last = ((int) placement.destination()
                .bottom() - 1) / BAND;
            for (int band = first; band <= last; band++) bands.computeIfAbsent(band, ignored -> new ArrayList<>())
                .add(placement);
        }
    }

    @Override
    public CaptureOutputSize size() {
        return size;
    }

    @Override
    public void row(int y, int[] target) throws IOException {
        Arrays.fill(target, background);
        for (Placement placement : bands.getOrDefault(y / BAND, List.of())) {
            CaptureRect rectangle = placement.destination();
            if (y < rectangle.y() || y >= rectangle.bottom()) continue;
            int width = (int) rectangle.width();
            int left = Math.max(0, (int) rectangle.x());
            int right = Math.min(size.width(), (int) rectangle.right());
            if (right <= left) continue;
            int sourceY = (int) ((long) (y - (int) rectangle.y()) * placement.source()
                .size()
                .height() / (int) rectangle.height());
            int sourceWidth = placement.source()
                .size()
                .width();
            if (width == sourceWidth) placement.source()
                .readSpan(sourceY, left - (int) rectangle.x(), right - left, target, left, buffer);
            else {
                placement.source()
                    .readSpan(sourceY, 0, sourceWidth, sampled, 0, buffer);
                for (int x = left; x < right; x++)
                    target[x] = sampled[(int) ((long) (x - (int) rectangle.x()) * sourceWidth / width)];
            }
        }
    }

    public long coveredPixels() {
        List<CaptureRect> rectangles = placements.stream()
            .map(
                p -> p.destination()
                    .intersect(new CaptureRect(0, 0, size.width(), size.height())))
            .filter(rect -> !rect.isEmpty())
            .toList();
        TreeSet<Integer> boundaries = new TreeSet<>();
        for (CaptureRect rect : rectangles) {
            boundaries.add((int) rect.y());
            boundaries.add((int) rect.bottom());
        }
        List<Integer> ys = List.copyOf(boundaries);
        long area = 0;
        for (int index = 1; index < ys.size(); index++) {
            int top = ys.get(index - 1);
            int bottom = ys.get(index);
            List<CaptureRect> row = rectangles.stream()
                .filter(rect -> rect.y() <= top && rect.bottom() >= bottom)
                .sorted(Comparator.comparingDouble(CaptureRect::x))
                .toList();
            int covered = 0;
            long width = 0;
            for (CaptureRect rect : row) {
                int left = Math.max(covered, (int) rect.x());
                int right = (int) rect.right();
                width += Math.max(0, right - left);
                covered = Math.max(covered, right);
            }
            area += width * (bottom - top);
        }
        return area;
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (Placement placement : placements) {
            try {
                placement.source()
                    .close();
            } catch (IOException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        bands.clear();
        if (failure != null) throw failure;
    }
}
