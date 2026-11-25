package com.hfstudio.guidenh.client.capture.export;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class CaptureSpritePageRows implements CaptureRows {

    private final CaptureSpritePageLayout layout;
    private final CaptureOutputSize frameSize;
    private final CaptureOutputSize size;
    private final IntFunction<Path> frames;
    private final int frameCount;
    private final List<BufferedImage> band = new ArrayList<>();
    private int bandIndex = -1;

    public CaptureSpritePageRows(CaptureSpritePageLayout layout, CaptureOutputSize frameSize, List<Path> frames) {
        this(layout, frameSize, frames.size(), List.copyOf(frames)::get);
    }

    public CaptureSpritePageRows(CaptureSpritePageLayout layout, CaptureOutputSize frameSize, int frameCount,
        IntFunction<Path> frames) {
        if (frameCount < 1 || frameCount > layout.framesPerPage())
            throw new IllegalArgumentException("A sprite page requires frames within its layout capacity");
        this.layout = layout;
        this.frameSize = frameSize;
        this.frames = Objects.requireNonNull(frames);
        this.frameCount = frameCount;
        size = new CaptureOutputSize(layout.width(), layout.height(frameCount));
    }

    @Override
    public CaptureOutputSize size() {
        return size;
    }

    @Override
    public void row(int y, int[] target) throws IOException {
        if (Thread.currentThread()
            .isInterrupted()) throw new IOException("Sprite export cancelled");
        Arrays.fill(target, 0);
        int relativeY = y - layout.padding();
        if (relativeY < 0) return;
        int nextBand = relativeY / layout.strideHeight();
        int frameY = relativeY % layout.strideHeight() - layout.offsetY();
        if (frameY < 0 || frameY >= frameSize.height()) return;
        if (bandIndex != nextBand) {
            releaseBand();
            int first = Math.multiplyExact(nextBand, layout.columns());
            try {
                int end = first + Math.min(layout.columns(), frameCount - first);
                for (int index = first; index < end; index++) band.add(read(frames.apply(index)));
                bandIndex = nextBand;
            } catch (IOException | RuntimeException | Error failure) {
                releaseBand();
                throw failure;
            }
        }
        for (int column = 0; column < band.size(); column++) band.get(column)
            .getRGB(0, frameY, frameSize.width(), 1, target, layout.x(column), frameSize.width());
    }

    private BufferedImage read(Path file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) throw new IOException("An intermediate sprite frame could not be opened");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("No decoder is available for an intermediate sprite frame");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                reader.addIIOReadProgressListener(new CaptureImageReadCancellation());
                if (reader.getWidth(0) != frameSize.width() || reader.getHeight(0) != frameSize.height())
                    throw new IOException("An intermediate sprite frame no longer matches its captured dimensions");
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("An intermediate sprite frame could not be decoded");
                if (Thread.currentThread()
                    .isInterrupted()) {
                    image.flush();
                    throw new IOException("Sprite export cancelled");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void releaseBand() {
        for (BufferedImage image : band) image.flush();
        band.clear();
        bandIndex = -1;
    }

    @Override
    public void close() {
        releaseBand();
    }
}
