package com.hfstudio.guidenh.client.capture.export;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.client.capture.api.CaptureAnimationWriter;
import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputArtifact;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordedFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRecordingOptions;
import com.hfstudio.guidenh.client.capture.export.CaptureAnimationManifest.FrameEntry;

public class PngSequenceCaptureWriter implements CaptureAnimationWriter {

    private final CaptureExportRequest request;
    private final CaptureRecordingOptions options;
    private final Path target;
    private final PngCaptureExporter png = new PngCaptureExporter();
    private final List<Long> times = new ArrayList<>();
    private CaptureOutputSize size;
    private long bytes;

    public PngSequenceCaptureWriter(CaptureExportRequest request, CaptureRecordingOptions options, Path target) {
        this.request = request;
        this.options = options;
        this.target = target;
    }

    private Path framePath(int index) {
        return target.resolve(String.format("frame_%06d.png", index));
    }

    @Override
    public void append(CaptureRecordedFrame frame) throws IOException {
        CaptureOutputArtifact artifact = png.export(frame.request(request), frame.frame(), framePath(times.size()));
        size = new CaptureOutputSize(artifact.width(), artifact.height());
        times.add(times.isEmpty() ? 0L : frame.elapsedNanos());
        bytes += artifact.byteSize();
        checkBudget();
    }

    private void checkBudget() throws IOException {
        if (bytes > request.options()
            .temporaryBudgetBytes()) throw new IOException("The animation exceeds the configured disk budget");
    }

    @Override
    public CaptureOutputArtifact finish(long elapsedNanos) throws IOException {
        if (times.isEmpty()) throw new IOException("No GUI frames were captured");
        long duration = Math.max(elapsedNanos, times.getLast() + 1_000_000_000L / options.fps());
        List<FrameEntry> entries = options.format()
            .equals("sprites") ? spritePages(duration) : sequenceEntries(duration);
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .create();
        Path metadata = target.resolve("animation.json");
        try (Writer output = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
            gson.toJson(
                new CaptureAnimationManifest(
                    request.sourceId(),
                    size.width(),
                    size.height(),
                    duration,
                    request.regions(),
                    entries),
                output);
        }
        bytes += Files.size(metadata);
        checkBudget();
        return new CaptureOutputArtifact(target, options.format(), size.width(), size.height(), times.size(), bytes);
    }

    private long duration(int index, long endTime) {
        return (index + 1 < times.size() ? times.get(index + 1) : endTime) - times.get(index);
    }

    private List<FrameEntry> sequenceEntries(long endTime) {
        List<FrameEntry> entries = new ArrayList<>(times.size());
        for (int index = 0; index < times.size(); index++) entries.add(
            new FrameEntry(
                framePath(index).getFileName()
                    .toString(),
                0,
                0,
                size.width(),
                size.height(),
                times.get(index),
                duration(index, endTime)));
        return entries;
    }

    private List<FrameEntry> spritePages(long endTime) throws IOException {
        int padding = options.spritePadding();
        int cellWidth = Math.addExact(size.width(), padding);
        int cellHeight = Math.addExact(size.height(), padding);
        int columns = Math.min(options.spriteColumns(), (options.spritePageSize() - padding) / cellWidth);
        if (columns < 1 || cellHeight + padding > options.spritePageSize()) {
            throw new IOException("One frame exceeds the sprite page size; increase it or choose a PNG sequence");
        }
        int width = Math.addExact(columns * cellWidth, padding);
        long pageBudget = options.memoryBudgetBytes() - size.rgbaBytes();
        int rows = Math.min(
            (options.spritePageSize() - padding) / cellHeight,
            (int) Math.min(Integer.MAX_VALUE, (pageBudget / (width * 4L) - padding) / cellHeight));
        if (rows < 1)
            throw new IOException("Sprite pages exceed the memory budget; use a PNG sequence or smaller frames");
        int perPage = Math.multiplyExact(columns, rows);
        List<FrameEntry> entries = new ArrayList<>(times.size());
        for (int first = 0, page = 0; first < times.size(); first += perPage, page++) {
            if (Thread.currentThread()
                .isInterrupted()) throw new IOException("Sprite export cancelled");
            int count = Math.min(perPage, times.size() - first);
            int actualRows = (count + columns - 1) / columns;
            BufferedImage image = new BufferedImage(
                width,
                actualRows * cellHeight + padding,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            String name = String.format("sprites_%04d.png", page);
            try {
                for (int offset = 0; offset < count; offset++) {
                    int index = first + offset;
                    int x = padding + offset % columns * cellWidth;
                    int y = padding + offset / columns * cellHeight;
                    BufferedImage frame = ImageIO.read(framePath(index).toFile());
                    if (frame == null) throw new IOException("An intermediate PNG frame could not be read");
                    graphics.drawImage(frame, x, y, null);
                    frame.flush();
                    entries.add(
                        new FrameEntry(
                            name,
                            x,
                            y,
                            size.width(),
                            size.height(),
                            times.get(index),
                            duration(index, endTime)));
                }
            } finally {
                graphics.dispose();
            }
            Path output = target.resolve(name);
            if (!ImageIO.write(image, "png", output.toFile())) throw new IOException("No PNG encoder is available");
            image.flush();
            bytes += Files.size(output);
            checkBudget();
            for (int offset = 0; offset < count; offset++) {
                Path source = framePath(first + offset);
                bytes -= Files.size(source);
                Files.delete(source);
            }
        }
        return entries;
    }

    @Override
    public void close() {
        times.clear();
    }
}
