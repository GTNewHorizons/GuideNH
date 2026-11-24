package com.hfstudio.guidenh.client.capture.export;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchResult;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.render.CaptureTileStore;

public record CaptureScrollRaster(Path file, CaptureRect crop) {

    public static CaptureScrollRaster resolve(CaptureBatchResult result, CaptureScrollSlice slice, Path directory)
        throws IOException {
        Path source = directory.resolve(result.file());
        CaptureRect crop = slice.crop();
        if (!Files.isDirectory(source)) return new CaptureScrollRaster(source, crop);
        Path regions = source.resolve("regions.json");
        if (Files.exists(regions)) {
            try (Reader reader = Files.newBufferedReader(regions, StandardCharsets.UTF_8)) {
                for (var value : new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("regions")) {
                    JsonObject entry = value.getAsJsonObject();
                    if (entry.get("id")
                        .getAsLong() == slice.regionId())
                        return new CaptureScrollRaster(
                            source.resolve(
                                entry.get("file")
                                    .getAsString()),
                            crop);
                }
                throw new IOException("The scroll source region is missing from its PNG manifest");
            }
        }
        Path frames = source.resolve("frame_000000.png");
        if (Files.exists(frames)) return new CaptureScrollRaster(frames, crop);
        try (Reader reader = Files.newBufferedReader(source.resolve("animation.json"), StandardCharsets.UTF_8)) {
            JsonObject frame = new JsonParser().parse(reader)
                .getAsJsonObject()
                .getAsJsonArray("frames")
                .get(0)
                .getAsJsonObject();
            return new CaptureScrollRaster(
                source.resolve(
                    frame.get("file")
                        .getAsString()),
                crop.move(
                    frame.get("x")
                        .getAsInt(),
                    frame.get("y")
                        .getAsInt()));
        }
    }

    public CaptureTileStore decode(Path directory, long diskBudget, long memoryBudget) throws IOException {
        CaptureTileStore rows = new CaptureTileStore(
            directory,
            new CaptureOutputSize((int) crop.width(), (int) crop.height()),
            0,
            diskBudget);
        try {
            if (file.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".png") && CapturePngRowDecoder.streamingFormat(file))
                CapturePngRowDecoder.decode(file, crop, rows, memoryBudget);
            else buffered(rows, memoryBudget);
            return rows;
        } catch (IOException | RuntimeException error) {
            rows.close();
            throw error;
        }
    }

    private void buffered(CaptureTileStore rows, long memoryBudget) throws IOException {
        BufferedImage image;
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("No image reader for the scroll capture source");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                reader.addIIOReadProgressListener(new CaptureImageReadCancellation());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height * 4 > memoryBudget / 2) throw new IOException(
                    "This animation or PNG format exceeds the scroll decoder memory budget; use an RGBA PNG sequence");
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setSourceRegion(
                    new Rectangle((int) crop.x(), (int) crop.y(), (int) crop.width(), (int) crop.height()));
                image = reader.read(0, parameters);
            } finally {
                reader.dispose();
            }
        }
        try {
            int[] pixels = new int[image.getWidth()];
            for (int y = 0; y < image.getHeight(); y++) {
                if (Thread.currentThread()
                    .isInterrupted()) throw new IOException("Scroll decoding cancelled");
                image.getRGB(0, y, image.getWidth(), 1, pixels, 0, image.getWidth());
                rows.putRow(y, pixels);
            }
        } finally {
            image.flush();
            rows.releaseBuffer();
        }
    }
}
