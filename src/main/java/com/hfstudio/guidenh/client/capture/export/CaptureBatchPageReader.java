package com.hfstudio.guidenh.client.capture.export;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureBatchPageReader {

    private CaptureBatchPageReader() {}

    public static CaptureOutputSize size(CaptureContactSheetEntry entry, int maximumDimension) throws IOException {
        if (maximumDimension < 0) throw new IllegalArgumentException("The page dimension limit cannot be negative");
        try (ReaderState state = open(entry)) {
            Rectangle source = state.source();
            double scale = maximumDimension == 0 ? 1
                : Math.min(1, (double) maximumDimension / Math.max(source.width, source.height));
            return new CaptureOutputSize(
                Math.max(1, (int) Math.round(source.width * scale)),
                Math.max(1, (int) Math.round(source.height * scale)));
        }
    }

    public static CaptureRect destination(CaptureOutputSize page, CaptureOutputSize canvas) {
        if (page.width() > canvas.width() || page.height() > canvas.height())
            throw new IllegalArgumentException("The page does not fit its animation canvas");
        return new CaptureRect(
            (canvas.width() - page.width()) / 2,
            (canvas.height() - page.height()) / 2,
            page.width(),
            page.height());
    }

    public static CaptureFrame frame(CaptureContactSheetEntry entry, CaptureOutputSize page, CaptureOutputSize canvas,
        int backgroundArgb, long timestampNanos, long memoryBudget) throws IOException {
        try (ReaderState state = open(entry)) {
            Rectangle source = state.source();
            int sample = (int) Math.max(
                1,
                Math.ceil(Math.max((double) source.width / page.width(), (double) source.height / page.height())));
            CaptureOutputSize decodedSize = new CaptureOutputSize(
                (source.width + sample - 1) / sample,
                (source.height + sample - 1) / sample);
            if (canvas.rgbaBytes() + decodedSize.rgbaBytes() * 2 > memoryBudget || state.reader()
                .getWidth(0) * 16L > memoryBudget / 2)
                throw new IOException("Page decoding exceeds the memory budget; choose a smaller page size");
            ImageReadParam parameters = state.reader()
                .getDefaultReadParam();
            parameters.setSourceRegion(source);
            parameters.setSourceSubsampling(sample, sample, 0, 0);
            BufferedImage decoded = state.reader()
                .read(0, parameters);
            BufferedImage image = null;
            try {
                image = new BufferedImage(canvas.width(), canvas.height(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setComposite(AlphaComposite.Src);
                    graphics.setColor(new Color(backgroundArgb, true));
                    graphics.fillRect(0, 0, canvas.width(), canvas.height());
                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    CaptureRect destination = destination(page, canvas);
                    graphics.drawImage(
                        decoded,
                        (int) destination.x(),
                        (int) destination.y(),
                        page.width(),
                        page.height(),
                        null);
                } finally {
                    graphics.dispose();
                }
                int[] pixels = ((DataBufferInt) image.getRaster()
                    .getDataBuffer()).getData();
                return new CaptureFrame(canvas.width(), canvas.height(), timestampNanos, pixels);
            } finally {
                decoded.flush();
                if (image != null) image.flush();
            }
        }
    }

    private static ReaderState open(CaptureContactSheetEntry entry) throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(
            entry.source()
                .toFile());
        if (input == null) throw new IOException("The page image could not be opened: " + entry.source());
        ImageReader reader = null;
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("No image decoder is available for " + entry.source());
            reader = readers.next();
            reader.setInput(input, false, true);
            reader.addIIOReadProgressListener(new CaptureImageReadCancellation());
            CaptureRect source = new CaptureRect(0, 0, reader.getWidth(0), reader.getHeight(0));
            if (entry.sourceBounds() != null) source = source.intersect(entry.sourceBounds());
            if (source.isEmpty()) throw new IOException("The page crop lies outside its source image");
            int x = (int) Math.floor(source.x());
            int y = (int) Math.floor(source.y());
            Rectangle crop = new Rectangle(
                x,
                y,
                (int) Math.ceil(source.right()) - x,
                (int) Math.ceil(source.bottom()) - y);
            return new ReaderState(input, reader, crop);
        } catch (IOException | RuntimeException error) {
            if (reader != null) reader.dispose();
            try {
                input.close();
            } catch (IOException cleanup) {
                error.addSuppressed(cleanup);
            }
            throw error;
        }
    }

    public record ReaderState(ImageInputStream input, ImageReader reader, Rectangle source) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            try {
                reader.dispose();
            } finally {
                input.close();
            }
        }
    }
}
