package com.hfstudio.guidenh.client.capture.export;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRows;

public class CaptureContactSheetRows implements CaptureRows {

    public static final int PADDING = 8;
    public static final int CAPTION = 20;
    private final List<CaptureContactSheetEntry> entries;
    private final int columns;
    private final int thumbnailSize;
    private final int cellWidth;
    private final int cellHeight;
    private final int background;
    private final long memoryBudget;
    private final CaptureOutputSize size;
    private List<BufferedImage> band = List.of();
    private int bandIndex = -1;

    public CaptureContactSheetRows(List<CaptureContactSheetEntry> entries, int columns, int thumbnailSize,
        int background, long memoryBudget) {
        this.entries = List.copyOf(entries);
        this.columns = Math.min(columns, entries.size());
        this.thumbnailSize = thumbnailSize;
        cellWidth = thumbnailSize + PADDING * 2;
        cellHeight = cellWidth + CAPTION;
        this.background = background;
        this.memoryBudget = memoryBudget;
        size = new CaptureOutputSize(
            this.columns * cellWidth,
            (entries.size() + this.columns - 1) / this.columns * cellHeight);
        if ((long) this.columns * cellWidth * cellHeight * 4 + size.width() * 12L > memoryBudget / 2)
            throw new IllegalArgumentException(
                "Contact sheet thumbnails exceed the memory budget; reduce columns or thumbnail size");
    }

    @Override
    public CaptureOutputSize size() {
        return size;
    }

    @Override
    public void row(int y, int[] target) throws IOException {
        Arrays.fill(target, background);
        int next = y / cellHeight;
        if (next != bandIndex) {
            releaseBand();
            List<BufferedImage> images = new ArrayList<>(columns);
            band = images;
            for (int index = next * columns; index < Math.min(entries.size(), (next + 1) * columns); index++)
                images.add(thumbnail(entries.get(index)));
            bandIndex = next;
        }
        for (int index = 0; index < band.size(); index++) band.get(index)
            .getRGB(0, y % cellHeight, cellWidth, 1, target, index * cellWidth, cellWidth);
    }

    private BufferedImage thumbnail(CaptureContactSheetEntry entry) throws IOException {
        if (Thread.currentThread()
            .isInterrupted()) throw new IOException("Contact sheet cancelled");
        BufferedImage sampled;
        try (ImageInputStream input = ImageIO.createImageInputStream(
            entry.source()
                .toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException(
                "No image reader for " + entry.source()
                    .getFileName());
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                reader.addIIOReadProgressListener(new CaptureImageReadCancellation());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width * 16L > memoryBudget / 2)
                    throw new IOException("The source scanline exceeds the contact sheet memory budget");
                ImageReadParam parameters = reader.getDefaultReadParam();
                int sample = (int) Math.max(1, ((long) Math.max(width, height) + thumbnailSize - 1) / thumbnailSize);
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                sampled = reader.read(0, parameters);
                if (Thread.currentThread()
                    .isInterrupted()) {
                    sampled.flush();
                    throw new IOException("Contact sheet cancelled");
                }
            } finally {
                reader.dispose();
            }
        }
        BufferedImage cell = new BufferedImage(cellWidth, cellHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cell.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(new Color(background, true));
            graphics.fillRect(0, 0, cellWidth, cellHeight);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math
                .min((double) thumbnailSize / sampled.getWidth(), (double) thumbnailSize / sampled.getHeight());
            int width = Math.max(1, (int) Math.round(sampled.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(sampled.getHeight() * scale));
            graphics.drawImage(
                sampled,
                PADDING + (thumbnailSize - width) / 2,
                PADDING + (thumbnailSize - height) / 2,
                width,
                height,
                null);
            String label = entry.label();
            while (!label.isEmpty() && graphics.getFontMetrics()
                .stringWidth(label) > thumbnailSize)
                label = label.substring(0, label.offsetByCodePoints(label.length(), -1));
            int baseline = cellHeight - PADDING;
            graphics.setColor(Color.BLACK);
            graphics.drawString(label, PADDING, baseline + 1);
            graphics.setColor(Color.WHITE);
            graphics.drawString(label, PADDING, baseline);
        } finally {
            graphics.dispose();
            sampled.flush();
        }
        return cell;
    }

    private void releaseBand() {
        for (BufferedImage image : band) image.flush();
        band = List.of();
    }

    @Override
    public void close() {
        releaseBand();
        bandIndex = -1;
    }
}
