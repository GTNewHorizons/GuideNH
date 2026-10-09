package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class GuideSiteAnimatedPng {

    private GuideSiteAnimatedPng() {}

    public record Frame(BufferedImage image, int ticks) {

        public Frame {
            if (image == null || ticks <= 0) {
                throw new IllegalArgumentException("Animated PNG frames require an image and positive duration");
            }
        }
    }

    public static byte[] encode(List<Frame> frames) throws IOException {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Animated PNG requires at least one frame");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeLong(0x89504e470d0a1a0aL);
        int width = frames.getFirst()
            .image()
            .getWidth();
        int height = frames.getFirst()
            .image()
            .getHeight();
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        DataOutputStream header = new DataOutputStream(headerBytes);
        header.writeInt(width);
        header.writeInt(height);
        header.write(new byte[] { 8, 6, 0, 0, 0 });
        chunk(out, "IHDR", headerBytes.toByteArray());
        ByteArrayOutputStream controlBytes = new ByteArrayOutputStream();
        DataOutputStream control = new DataOutputStream(controlBytes);
        control.writeInt(frames.size());
        control.writeInt(0);
        chunk(out, "acTL", controlBytes.toByteArray());
        int sequence = 0;
        BufferedImage previous = null;
        var writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IOException("No PNG writer is available");
        }
        ImageWriter writer = writers.next();
        try {
            for (Frame frame : frames) {
                BufferedImage image = frame.image();
                if (image.getWidth() != width || image.getHeight() != height || frame.ticks() > 65535) {
                    throw new IllegalArgumentException("Animated PNG frame dimensions or duration are invalid");
                }
                int[] rect = changedRectangle(previous, image);
                ByteArrayOutputStream frameBytes = new ByteArrayOutputStream();
                DataOutputStream frameControl = new DataOutputStream(frameBytes);
                frameControl.writeInt(sequence++);
                frameControl.writeInt(rect[2]);
                frameControl.writeInt(rect[3]);
                frameControl.writeInt(rect[0]);
                frameControl.writeInt(rect[1]);
                frameControl.writeShort(frame.ticks());
                frameControl.writeShort(20);
                frameControl.writeByte(0);
                frameControl.writeByte(0);
                chunk(out, "fcTL", frameBytes.toByteArray());

                BufferedImage crop = new BufferedImage(rect[2], rect[3], BufferedImage.TYPE_INT_ARGB);
                copyRectangle(image, crop, rect);
                ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
                try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(pngBytes)) {
                    writer.setOutput(imageOutput);
                    ImageWriteParam parameters = writer.getDefaultWriteParam();
                    writer.write(null, new IIOImage(crop, null, null), parameters);
                } finally {
                    writer.reset();
                }
                DataInputStream png = new DataInputStream(new ByteArrayInputStream(pngBytes.toByteArray()));
                png.readLong();
                while (png.available() > 0) {
                    int length = png.readInt();
                    String type = new String(png.readNBytes(4), StandardCharsets.US_ASCII);
                    byte[] data = png.readNBytes(length);
                    png.readInt();
                    if (!"IDAT".equals(type)) continue;
                    if (previous == null) {
                        chunk(out, "IDAT", data);
                    } else {
                        ByteArrayOutputStream partBytes = new ByteArrayOutputStream();
                        DataOutputStream part = new DataOutputStream(partBytes);
                        part.writeInt(sequence++);
                        part.write(data);
                        chunk(out, "fdAT", partBytes.toByteArray());
                    }
                }
                previous = image;
            }
        } finally {
            writer.dispose();
        }
        chunk(out, "IEND", new byte[0]);
        return bytes.toByteArray();
    }

    private static int[] changedRectangle(BufferedImage previous, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (previous == null) return new int[] { 0, 0, width, height };
        int[] previousPixels = packedPixels(previous);
        int[] currentPixels = packedPixels(image);
        int left = width, right = -1, top = height, bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int previousPixel = previousPixels != null ? previousPixels[index] : previous.getRGB(x, y);
                int currentPixel = currentPixels != null ? currentPixels[index] : image.getRGB(x, y);
                if (previousPixel != currentPixel) {
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return right < 0 ? new int[] { 0, 0, 1, 1 } : new int[] { left, top, right - left + 1, bottom - top + 1 };
    }

    private static void copyRectangle(BufferedImage source, BufferedImage target, int[] rectangle) {
        int[] sourcePixels = packedPixels(source);
        int[] targetPixels = packedPixels(target);
        int width = rectangle[2];
        int height = rectangle[3];
        if (sourcePixels != null && targetPixels != null) {
            int sourceWidth = source.getWidth();
            for (int row = 0; row < height; row++) {
                System.arraycopy(
                    sourcePixels,
                    (rectangle[1] + row) * sourceWidth + rectangle[0],
                    targetPixels,
                    row * width,
                    width);
            }
            return;
        }
        target.setRGB(
            0,
            0,
            width,
            height,
            source.getRGB(rectangle[0], rectangle[1], width, height, null, 0, width),
            0,
            width);
    }

    private static int[] packedPixels(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB || image.getRaster()
            .getMinX() != 0
            || image.getRaster()
                .getMinY() != 0
            || !(image.getRaster()
                .getDataBuffer() instanceof DataBufferInt data)
            || data.getOffset() != 0) {
            return null;
        }
        int expectedLength = image.getWidth() * image.getHeight();
        int[] pixels = data.getData();
        return pixels.length == expectedLength ? pixels : null;
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
